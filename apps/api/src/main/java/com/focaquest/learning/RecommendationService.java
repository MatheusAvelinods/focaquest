package com.focaquest.learning;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.focaquest.auth.AppUser;
import com.focaquest.catalog.CatalogDtos.SubjectDto;
import com.focaquest.catalog.CatalogDtos.SyllabusDto;
import com.focaquest.catalog.CatalogDtos.TopicDto;
import com.focaquest.catalog.CatalogService;
import com.focaquest.learning.RecommendationEngine.Recommendation;
import com.focaquest.learning.RecommendationEngine.ReviewCandidate;
import com.focaquest.learning.RecommendationEngine.TopicCandidate;
import com.focaquest.progress.ProgressService;
import com.focaquest.progress.TopicState;
import com.focaquest.progress.UserTopicProgress;
import com.focaquest.questions.Question;
import com.focaquest.questions.QuestionRepository;
import com.focaquest.review.ReviewService;

/** Reúne as evidências do estudante e delega a decisão ao {@link RecommendationEngine}. */
@Service
public class RecommendationService {

    private final CatalogService catalog;
    private final LessonRepository lessons;
    private final QuestionRepository questions;
    private final ProgressService progress;
    private final ReviewService reviews;
    private final StudySessionService sessions;
    private final Clock clock;

    public RecommendationService(CatalogService catalog, LessonRepository lessons, QuestionRepository questions,
                                 ProgressService progress, ReviewService reviews, StudySessionService sessions,
                                 Clock clock) {
        this.catalog = catalog;
        this.lessons = lessons;
        this.questions = questions;
        this.progress = progress;
        this.reviews = reviews;
        this.sessions = sessions;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Recommendation recommend(AppUser user) {
        if (user.getSelectedRoleId() == null) {
            return Recommendation.NONE;
        }
        UUID userId = user.getId();
        SyllabusDto syllabus = catalog.syllabus(user.getSelectedRoleId());
        Map<String, UserTopicProgress> mine = progress.byTopic(userId);
        List<String> topicIds = syllabus.subjects().stream().flatMap(s -> s.topics().stream()).map(TopicDto::id).toList();
        Map<String, List<Lesson>> lessonsByTopic = topicIds.isEmpty() ? Map.of()
                : lessons.findByTopicIdInAndStatusOrderByPositionAsc(topicIds, Lesson.PUBLISHED).stream()
                        .collect(Collectors.groupingBy(Lesson::getTopicId));
        String board = syllabus.exam().board();

        List<TopicCandidate> candidates = new ArrayList<>();
        for (SubjectDto s : syllabus.subjects()) {
            for (TopicDto t : s.topics()) {
                List<Lesson> topicLessons = lessonsByTopic.getOrDefault(t.id(), List.of());
                Lesson next = topicLessons.stream().filter(l -> !sessions.lessonCompleted(userId, l.getId()))
                        .findFirst().orElse(null);
                Lesson first = topicLessons.isEmpty() ? null : topicLessons.getFirst();
                UserTopicProgress p = mine.get(t.id());
                long boardCount = t.questionCount() == 0 ? 0
                        : questions.countByTopicIdAndStatusAndBoardStyle(t.id(), Question.Status.PUBLISHED, board);
                candidates.add(new TopicCandidate(t.id(), t.name(), s.id(), s.name(),
                        p == null ? TopicState.NOT_STARTED : p.getState(),
                        p == null ? 0 : p.getAttempts(), p == null ? 0 : p.accuracy(),
                        next == null ? null : next.getId(), next == null ? null : next.getTitle(),
                        first == null ? null : first.getId(), first == null ? null : first.getTitle(),
                        (int) t.questionCount(), boardCount));
            }
        }
        List<ReviewCandidate> reviewCandidates = reviews.all(userId).stream()
                .map(r -> new ReviewCandidate(r.getId(), r.getTopicId(), r.getDueAt())).toList();

        return RecommendationEngine.recommend(new RecommendationEngine.Input(
                user.getStrategy(), user.getStrategySubjectId(), user.getPreferredMissionType(), board,
                candidates, reviewCandidates, sessions.recentCompletedKinds(userId, 2), clock.instant()));
    }
}
