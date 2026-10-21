package com.focaquest.learning;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.focaquest.auth.AppUser;
import com.focaquest.auth.AuthService;
import com.focaquest.catalog.CatalogDtos.ExamDto;
import com.focaquest.catalog.CatalogDtos.RoleDto;
import com.focaquest.catalog.CatalogService;
import com.focaquest.learning.LearningDtos.InterruptedSession;
import com.focaquest.learning.RecommendationEngine.Recommendation;
import com.focaquest.progress.ProgressDtos.ProgressSummary;
import com.focaquest.progress.ProgressService;
import com.focaquest.review.ReviewSchedule;
import com.focaquest.review.ReviewService;

/** Monta a tela "Minha jornada": responde "o que estudar agora?" em uma única chamada. */
@Service
public class JourneyService {

    private final AuthService auth;
    private final CatalogService catalog;
    private final RecommendationService recommendations;
    private final StudySessionService sessions;
    private final LessonService lessons;
    private final ProgressService progress;
    private final ReviewService reviews;
    private final Clock clock;

    public JourneyService(AuthService auth, CatalogService catalog, RecommendationService recommendations,
                          StudySessionService sessions, LessonService lessons, ProgressService progress,
                          ReviewService reviews, Clock clock) {
        this.auth = auth;
        this.catalog = catalog;
        this.recommendations = recommendations;
        this.sessions = sessions;
        this.lessons = lessons;
        this.progress = progress;
        this.reviews = reviews;
        this.clock = clock;
    }

    public record JourneyProgress(double coveragePercent, int topicsStarted, int topicsMastered, int topicsTotal,
                                  int attempts, double accuracy, long xp) {
    }

    public record PendingReview(UUID reviewId, String topicId, String topicName, Instant dueAt, boolean due) {
    }

    public record Journey(boolean needsOnboarding, String displayName, ExamDto exam, RoleDto role,
                          Mission recommended, Mission shortAlternative, InterruptedSession interrupted,
                          JourneyProgress progress, long dueReviews, List<PendingReview> upcomingReviews) {
    }

    @Transactional(readOnly = true)
    public Journey journey(UUID userId) {
        AppUser user = auth.get(userId);
        if (user.getSelectedRoleId() == null) {
            return new Journey(true, user.getDisplayName(), null, null, null, null, null, null, 0, List.of());
        }
        RoleDto role = catalog.listRoles(catalog.requireRole(user.getSelectedRoleId()).getExamId()).stream()
                .filter(r -> r.id().equals(user.getSelectedRoleId())).findFirst().orElseThrow();
        ExamDto exam = catalog.listExams().stream().filter(e -> e.id().equals(role.examId())).findFirst().orElseThrow();

        Recommendation rec = recommendations.recommend(user);
        InterruptedSession interrupted = sessions.interrupted(userId).map(this::toInterrupted).orElse(null);
        ProgressSummary summary = progress.summary(userId, user.getSelectedRoleId());

        Instant now = clock.instant();
        List<ReviewSchedule> all = reviews.all(userId);
        List<PendingReview> upcoming = all.stream().limit(5).map(r -> pending(r, now)).toList();

        return new Journey(false, user.getDisplayName(), exam, role, rec.primary(), rec.alternative(), interrupted,
                new JourneyProgress(summary.edital().coveragePercent(), summary.edital().topicsStarted(),
                        summary.edital().topicsMastered(), summary.edital().topicsTotal(),
                        summary.questions().attempts(), summary.questions().accuracy(), summary.xp()),
                summary.reviews().due(), upcoming);
    }

    @Transactional(readOnly = true)
    public List<PendingReview> reviews(UUID userId) {
        Instant now = clock.instant();
        return reviews.all(userId).stream().map(r -> pending(r, now)).toList();
    }

    private PendingReview pending(ReviewSchedule r, Instant now) {
        return new PendingReview(r.getId(), r.getTopicId(), lessons.topicRef(r.getTopicId()).name(), r.getDueAt(),
                r.isDue(now));
    }

    private InterruptedSession toInterrupted(StudySession s) {
        String lessonTitle = s.getLessonId() == null ? null : lessons.requirePublished(s.getLessonId()).getTitle();
        return new InterruptedSession(s.getId(), s.getKind(), s.getTopicId(), s.getLessonId(), s.getReviewId(),
                lessons.topicRef(s.getTopicId()).name(), lessonTitle,
                s.getCurrentStep(), s.steps().size(), sessions.answeredCount(s.getId()), s.getQuestionIds().size(),
                s.getLastActivityAt());
    }
}
