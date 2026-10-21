package com.focaquest.learning;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.focaquest.learning.LearningDtos.AnswerResponse;
import com.focaquest.learning.LearningDtos.SessionResult;
import com.focaquest.learning.LearningDtos.SessionView;
import com.focaquest.learning.LearningDtos.StartSessionRequest;
import com.focaquest.learning.LearningDtos.StepDto;
import com.focaquest.learning.LearningDtos.UpdateSessionRequest;
import com.focaquest.progress.ProgressDtos.StateChange;
import com.focaquest.progress.ProgressService;
import com.focaquest.progress.TopicState;
import com.focaquest.progress.UserTopicProgress;
import com.focaquest.questions.Question;
import com.focaquest.questions.QuestionRepository;
import com.focaquest.questions.QuestionViews.Feedback;
import com.focaquest.questions.QuestionViews.PublicQuestion;
import com.focaquest.review.ReviewSchedule;
import com.focaquest.review.ReviewService;
import com.focaquest.shared.BusinessRuleException;
import com.focaquest.shared.MissionType;
import com.focaquest.shared.NotFoundException;

@Service
public class StudySessionService {

    private static final Logger log = LoggerFactory.getLogger(StudySessionService.class);
    private static final EnumSet<SessionStatus> OPEN = EnumSet.of(SessionStatus.IN_PROGRESS, SessionStatus.PAUSED);

    private final StudySessionRepository sessions;
    private final AnswerAttemptRepository attempts;
    private final QuestionRepository questions;
    private final LessonService lessons;
    private final LessonRepository lessonRepository;
    private final ProgressService progress;
    private final ReviewService reviews;
    private final Clock clock;

    public StudySessionService(StudySessionRepository sessions, AnswerAttemptRepository attempts,
                               QuestionRepository questions, LessonService lessons, LessonRepository lessonRepository,
                               ProgressService progress, ReviewService reviews, Clock clock) {
        this.sessions = sessions;
        this.attempts = attempts;
        this.questions = questions;
        this.lessons = lessons;
        this.lessonRepository = lessonRepository;
        this.progress = progress;
        this.reviews = reviews;
        this.clock = clock;
    }

    @Transactional
    public SessionView start(UUID userId, StartSessionRequest req) {
        Instant now = clock.instant();
        MissionType type = req.missionType() == null ? MissionType.STANDARD : req.missionType();

        String topicId;
        String lessonId = null;
        UUID reviewId = null;
        List<String> selected;
        switch (req.kind()) {
            case LESSON -> {
                if (req.lessonId() == null) throw new IllegalArgumentException("lessonId é obrigatório para lições.");
                Lesson lesson = lessons.requirePublished(req.lessonId());
                topicId = lesson.getTopicId();
                lessonId = lesson.getId();
                selected = QuestionSelector.forLesson(published(lessons.questionIds(lessonId)),
                        topicQuestionIds(topicId), history(userId, topicId), type, now);
            }
            case REINFORCEMENT -> {
                if (req.topicId() == null) throw new IllegalArgumentException("topicId é obrigatório para reforço.");
                topicId = lessons.topicRef(req.topicId()).id();
                lessonId = lessonRepository.findByTopicIdAndStatusOrderByPositionAsc(topicId, Lesson.PUBLISHED)
                        .stream().findFirst().map(Lesson::getId).orElse(null);
                selected = QuestionSelector.forPractice(topicQuestionIds(topicId), history(userId, topicId), type, now);
            }
            case REVIEW -> {
                if (req.reviewId() == null) throw new IllegalArgumentException("reviewId é obrigatório para revisões.");
                ReviewSchedule review = reviews.require(userId, req.reviewId());
                topicId = review.getTopicId();
                reviewId = review.getId();
                selected = QuestionSelector.forPractice(topicQuestionIds(topicId), history(userId, topicId), type, now);
            }
            default -> throw new IllegalStateException();
        }
        if (selected.isEmpty()) {
            throw new BusinessRuleException("Ainda não há questões publicadas para este assunto.");
        }

        // Retomar em vez de duplicar: se a mesma missão já está aberta, devolve-a.
        String finalLessonId = lessonId;
        UUID finalReviewId = reviewId;
        String finalTopicId = topicId;
        var existing = sessions.findByUserIdAndStatusIn(userId, OPEN).stream()
                .filter(s -> s.getKind() == req.kind() && s.getTopicId().equals(finalTopicId)
                        && Objects.equals(s.getLessonId(), finalLessonId)
                        && Objects.equals(s.getReviewId(), finalReviewId))
                .findFirst();
        if (existing.isPresent()) {
            existing.get().touch(now);
            return view(existing.get());
        }
        // Só uma missão fica "em andamento" por vez; as demais ficam pausadas e não se perdem.
        sessions.findByUserIdAndStatusIn(userId, EnumSet.of(SessionStatus.IN_PROGRESS)).forEach(s -> s.pause(now));

        StudySession session = sessions.save(
                new StudySession(userId, req.kind(), type, topicId, lessonId, reviewId, selected, now));
        log.info("session.start user={} session={} kind={} topic={} questions={}", userId, session.getId(),
                req.kind(), topicId, selected.size());
        return view(session);
    }

    @Transactional(readOnly = true)
    public SessionView get(UUID userId, UUID sessionId) {
        return view(require(userId, sessionId));
    }

    @Transactional
    public AnswerResponse answer(UUID userId, UUID sessionId, String questionId, String selectedOption) {
        StudySession session = require(userId, sessionId);
        String option = selectedOption.toUpperCase();
        int step = session.stepOf(questionId);
        Question question = questions.findById(questionId).orElseThrow(() -> new NotFoundException("Questão não encontrada."));

        var previous = attempts.findBySessionIdAndQuestionId(sessionId, questionId);
        if (previous.isPresent()) {
            // Integridade da tentativa: a primeira resposta é a que vale.
            return new AnswerResponse(Feedback.of(question, previous.get().getSelectedOption()), true, 0,
                    topicState(userId, session.getTopicId()), session.getCurrentStep());
        }
        if (!session.isOpen()) {
            throw new BusinessRuleException("Esta missão já foi concluída.");
        }
        if (!question.hasOption(option)) {
            throw new IllegalArgumentException("Alternativa inexistente: " + option);
        }
        Instant now = clock.instant();
        boolean correct = question.isCorrect(option);
        attempts.save(new AnswerAttempt(sessionId, userId, questionId, question.getRevision(), question.getTopicId(),
                option, correct, now));
        if (session.getCurrentStep() < step) {
            session.moveTo(step, now);
        } else {
            session.touch(now);
        }
        StateChange change = progress.recordAnswer(userId, question.getTopicId(), correct);
        int xp = progress.awardXp(userId, SessionXp.forAnswer(correct), "ANSWER", sessionId + ":" + questionId);
        return new AnswerResponse(Feedback.of(question, option), false, xp, change.current(), session.getCurrentStep());
    }

    @Transactional
    public SessionView update(UUID userId, UUID sessionId, UpdateSessionRequest req) {
        StudySession session = require(userId, sessionId);
        Instant now = clock.instant();
        if (req.currentStep() != null && session.isOpen()) {
            session.moveTo(req.currentStep(), now);
        }
        if (req.action() != null) {
            switch (req.action()) {
                case PAUSE -> session.pause(now);
                case RESUME -> {
                    boolean wasPaused = session.getStatus() == SessionStatus.PAUSED;
                    session.resume(now);
                    if (wasPaused) {
                        progress.awardXp(userId, SessionXp.RESUME_BONUS, "RESUME", session.getId().toString());
                    }
                }
                case COMPLETE -> complete(session, now);
            }
        }
        return view(session);
    }

    /** Conclui a missão de revisão indicada. Idempotente. */
    @Transactional
    public SessionView completeReview(UUID userId, UUID reviewId, UUID sessionId) {
        StudySession session = require(userId, sessionId);
        if (session.getKind() != SessionKind.REVIEW || !reviewId.equals(session.getReviewId())) {
            throw new BusinessRuleException("A sessão informada não pertence a esta revisão.");
        }
        if (session.isOpen()) {
            complete(session, clock.instant());
        }
        return view(session);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<StudySession> interrupted(UUID userId) {
        return sessions.findFirstByUserIdAndStatusInOrderByLastActivityAtDesc(userId, OPEN);
    }

    @Transactional(readOnly = true)
    public List<SessionKind> recentCompletedKinds(UUID userId, int limit) {
        return sessions.findByUserIdAndStatusOrderByCompletedAtDesc(userId, SessionStatus.COMPLETED).stream()
                .limit(limit).map(StudySession::getKind).toList();
    }

    @Transactional(readOnly = true)
    public boolean lessonCompleted(UUID userId, String lessonId) {
        return sessions.existsByUserIdAndLessonIdAndStatus(userId, lessonId, SessionStatus.COMPLETED);
    }

    @Transactional(readOnly = true)
    public int answeredCount(UUID sessionId) {
        return attempts.findBySessionId(sessionId).size();
    }

    private void complete(StudySession session, Instant now) {
        List<AnswerAttempt> answered = attempts.findBySessionId(session.getId());
        int total = session.getQuestionIds().size();
        if (answered.size() < total) {
            throw new BusinessRuleException("Responda todas as questões antes de concluir a missão ("
                    + answered.size() + "/" + total + ").");
        }
        UUID userId = session.getUserId();
        double score = total == 0 ? 0 : (double) answered.stream().filter(AnswerAttempt::isCorrect).count() / total;

        StateChange change;
        if (session.getKind() == SessionKind.REVIEW) {
            reviews.complete(userId, session.getReviewId(), session.getId(), score);
            change = progress.refresh(userId, session.getTopicId());
        } else {
            reviews.ensureScheduled(userId, session.getTopicId());
            change = progress.recordStudyCompleted(userId, session.getTopicId());
        }
        int xp = progress.awardXp(userId, SessionXp.forCompletion(session.getMissionType(), session.getKind()),
                "MISSION", session.getId().toString());
        if (change.masteryMilestone()) {
            xp += progress.awardXp(userId, SessionXp.MASTERY_MILESTONE, "MASTERY", session.getTopicId());
        }
        int answerXp = answered.stream().mapToInt(a -> SessionXp.forAnswer(a.isCorrect())).sum();
        session.complete(xp + answerXp, now);
        log.info("session.complete user={} session={} kind={} score={} state={}->{}", userId, session.getId(),
                session.getKind(), score, change.previous(), change.current());
    }

    private StudySession require(UUID userId, UUID sessionId) {
        return sessions.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new NotFoundException("Missão não encontrada."));
    }

    private SessionView view(StudySession s) {
        Map<String, Question> qs = questions.findByIdIn(s.getQuestionIds()).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        Map<String, AnswerAttempt> answered = attempts.findBySessionId(s.getId()).stream()
                .collect(Collectors.toMap(AnswerAttempt::getQuestionId, Function.identity()));

        List<StepDto> steps = new ArrayList<>();
        var types = s.steps();
        int qIndex = 0;
        for (int i = 0; i < types.size(); i++) {
            var type = types.get(i);
            if (type == StudySession.StepType.QUESTION) {
                Question q = qs.get(s.getQuestionIds().get(qIndex++));
                AnswerAttempt a = answered.get(q.getId());
                steps.add(new StepDto(i, type, PublicQuestion.of(q), a == null ? null : Feedback.of(q, a.getSelectedOption())));
            } else {
                steps.add(new StepDto(i, type, null, null));
            }
        }
        var lesson = s.getLessonId() == null ? null
                : lessonRepository.findById(s.getLessonId()).map(lessons::content).orElse(null);

        SessionResult result = null;
        if (s.getStatus() == SessionStatus.COMPLETED) {
            int correct = (int) answered.values().stream().filter(AnswerAttempt::isCorrect).count();
            int total = s.getQuestionIds().size();
            UserTopicProgress p = progress.byTopic(s.getUserId()).get(s.getTopicId());
            boolean milestone = p != null && p.getMasteryReachedAt() != null
                    && !p.getMasteryReachedAt().isBefore(s.getStartedAt());
            Instant nextReview = reviews.find(s.getUserId(), s.getTopicId()).map(ReviewSchedule::getDueAt).orElse(null);
            result = new SessionResult(correct, total, total == 0 ? 0 : (double) correct / total, s.getXpAwarded(),
                    p == null ? TopicState.NOT_STARTED : p.getState(), milestone, nextReview);
        }
        return new SessionView(s.getId(), s.getKind(), s.getMissionType(), s.getStatus(), s.getCurrentStep(),
                s.getStartedAt(), s.getLastActivityAt(), lessons.topicRef(s.getTopicId()),
                s.presentsLesson() ? lesson : null, s.getReviewId(), steps, answered.size(),
                s.getQuestionIds().size(), result);
    }

    private TopicState topicState(UUID userId, String topicId) {
        UserTopicProgress p = progress.byTopic(userId).get(topicId);
        return p == null ? TopicState.NOT_STARTED : p.getState();
    }

    private List<String> topicQuestionIds(String topicId) {
        return questions.findByTopicIdAndStatusOrderByIdAsc(topicId, Question.Status.PUBLISHED).stream()
                .map(Question::getId).toList();
    }

    private List<String> published(List<String> ids) {
        var publishedIds = questions.findByIdIn(ids).stream()
                .filter(q -> q.getStatus() == Question.Status.PUBLISHED).map(Question::getId)
                .collect(Collectors.toSet());
        return ids.stream().filter(publishedIds::contains).toList();
    }

    private Map<String, QuestionSelector.History> history(UUID userId, String topicId) {
        Map<String, QuestionSelector.History> out = new HashMap<>();
        // Ordenado do mais recente para o mais antigo: a primeira ocorrência é a última tentativa.
        for (AnswerAttempt a : attempts.findByUserIdAndTopicIdOrderByAnsweredAtDesc(userId, topicId)) {
            out.putIfAbsent(a.getQuestionId(), new QuestionSelector.History(a.getAnsweredAt(), a.isCorrect()));
        }
        return out;
    }
}
