package com.focaquest.progress;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.focaquest.catalog.CatalogDtos.SubjectDto;
import com.focaquest.catalog.CatalogDtos.SyllabusDto;
import com.focaquest.catalog.CatalogDtos.TopicDto;
import com.focaquest.catalog.CatalogService;
import com.focaquest.progress.ProgressDtos.EditalProgress;
import com.focaquest.progress.ProgressDtos.ProgressSummary;
import com.focaquest.progress.ProgressDtos.QuestionStats;
import com.focaquest.progress.ProgressDtos.ReviewStats;
import com.focaquest.progress.ProgressDtos.StateChange;
import com.focaquest.progress.ProgressDtos.SubjectProgressDto;
import com.focaquest.progress.ProgressDtos.TopicProgressDto;
import com.focaquest.review.ReviewSchedule;
import com.focaquest.review.ReviewService;

@Service
public class ProgressService {

    private final UserTopicProgressRepository progress;
    private final XpEventRepository xpEvents;
    private final ReviewService reviews;
    private final CatalogService catalog;
    private final Clock clock;

    public ProgressService(UserTopicProgressRepository progress, XpEventRepository xpEvents, ReviewService reviews,
                           CatalogService catalog, Clock clock) {
        this.progress = progress;
        this.xpEvents = xpEvents;
        this.reviews = reviews;
        this.catalog = catalog;
        this.clock = clock;
    }

    @Transactional
    public StateChange recordAnswer(UUID userId, String topicId, boolean correct) {
        UserTopicProgress p = load(userId, topicId);
        TopicState before = p.getState();
        p.recordAnswer(correct, clock.instant());
        return recompute(p, before);
    }

    @Transactional
    public StateChange recordStudyCompleted(UUID userId, String topicId) {
        UserTopicProgress p = load(userId, topicId);
        TopicState before = p.getState();
        p.recordLessonCompleted(clock.instant());
        return recompute(p, before);
    }

    /** Recalcula o estado após uma mudança externa, como a conclusão de uma revisão. */
    @Transactional
    public StateChange refresh(UUID userId, String topicId) {
        UserTopicProgress p = load(userId, topicId);
        return recompute(p, p.getState());
    }

    @Transactional(readOnly = true)
    public Map<String, UserTopicProgress> byTopic(UUID userId) {
        return progress.findByUserId(userId).stream()
                .collect(Collectors.toMap(UserTopicProgress::getTopicId, Function.identity()));
    }

    /** Concede XP uma única vez por (motivo, referência). Retorna o valor efetivamente concedido. */
    @Transactional
    public int awardXp(UUID userId, int amount, String reason, String referenceId) {
        if (amount <= 0 || xpEvents.existsByUserIdAndReasonAndReferenceId(userId, reason, referenceId)) {
            return 0;
        }
        xpEvents.save(new XpEvent(userId, amount, reason, referenceId, clock.instant()));
        return amount;
    }

    @Transactional(readOnly = true)
    public long totalXp(UUID userId) {
        return xpEvents.totalFor(userId);
    }

    @Transactional(readOnly = true)
    public ProgressSummary summary(UUID userId, String roleId) {
        SyllabusDto syllabus = catalog.syllabus(roleId);
        Map<String, UserTopicProgress> mine = byTopic(userId);
        List<SubjectProgressDto> subjects = new ArrayList<>();
        List<TopicProgressDto> topics = new ArrayList<>();
        int total = 0, started = 0, mastered = 0, attempts = 0, correct = 0;

        for (SubjectDto s : syllabus.subjects()) {
            int sTotal = 0, sStarted = 0, sMastered = 0, sAttempts = 0, sCorrect = 0;
            for (TopicDto t : s.topics()) {
                UserTopicProgress p = mine.get(t.id());
                TopicState state = p == null ? TopicState.NOT_STARTED : p.getState();
                sTotal++;
                if (state != TopicState.NOT_STARTED) sStarted++;
                if (state == TopicState.MASTERED) sMastered++;
                if (p != null) {
                    sAttempts += p.getAttempts();
                    sCorrect += p.getCorrect();
                }
                topics.add(new TopicProgressDto(t.id(), s.id(), t.name(), state,
                        p == null ? 0 : p.getAttempts(), p == null ? 0 : p.getCorrect(),
                        p == null ? 0 : p.accuracy(), p == null ? null : p.getLastStudiedAt(),
                        p == null ? null : p.getMasteryReachedAt()));
            }
            subjects.add(new SubjectProgressDto(s.id(), s.name(), s.area(), sTotal, sStarted, sMastered, sAttempts,
                    sCorrect, ratio(sCorrect, sAttempts)));
            total += sTotal;
            started += sStarted;
            mastered += sMastered;
            attempts += sAttempts;
            correct += sCorrect;
        }

        Instant now = clock.instant();
        List<ReviewSchedule> all = reviews.all(userId);
        long due = all.stream().filter(r -> r.isDue(now)).count();
        return new ProgressSummary(totalXp(userId),
                new EditalProgress(total, started, mastered, total == 0 ? 0 : round(100.0 * started / total)),
                new QuestionStats(attempts, correct, ratio(correct, attempts)),
                new ReviewStats(due, all.size()),
                subjects, topics);
    }

    private UserTopicProgress load(UUID userId, String topicId) {
        return progress.findById(new UserTopicProgress.Key(userId, topicId))
                .orElseGet(() -> progress.save(new UserTopicProgress(userId, topicId)));
    }

    private StateChange recompute(UserTopicProgress p, TopicState before) {
        var schedule = reviews.find(p.getUserId(), p.getTopicId());
        boolean milestone = p.recompute(schedule.isPresent(),
                schedule.map(ReviewSchedule::getSuccessfulReviews).orElse(0), clock.instant());
        return new StateChange(p.getTopicId(), before, p.getState(), milestone);
    }

    private static double ratio(int a, int b) {
        return b == 0 ? 0 : round((double) a / b);
    }

    private static double round(double v) {
        return Math.round(v * 1000) / 1000.0;
    }

}
