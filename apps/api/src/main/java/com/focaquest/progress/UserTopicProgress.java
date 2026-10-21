package com.focaquest.progress;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_topic_progress")
@IdClass(UserTopicProgress.Key.class)
public class UserTopicProgress {

    @Id
    @Column(name = "user_id")
    private UUID userId;
    @Id
    @Column(name = "topic_id")
    private String topicId;
    @Enumerated(EnumType.STRING)
    private TopicState state = TopicState.NOT_STARTED;
    @Column(name = "lessons_completed")
    private int lessonsCompleted;
    private int attempts;
    private int correct;
    @Column(name = "recent_results")
    private String recentResults = "";
    @Column(name = "last_studied_at")
    private Instant lastStudiedAt;
    @Column(name = "mastery_reached_at")
    private Instant masteryReachedAt;

    protected UserTopicProgress() {
    }

    public UserTopicProgress(UUID userId, String topicId) {
        this.userId = userId;
        this.topicId = topicId;
    }

    void recordAnswer(boolean wasCorrect, Instant at) {
        attempts += 1;
        if (wasCorrect) {
            correct += 1;
        }
        recentResults = MasteryCalculator.append(recentResults, wasCorrect);
        lastStudiedAt = at;
    }

    void recordLessonCompleted(Instant at) {
        lessonsCompleted += 1;
        lastStudiedAt = at;
    }

    /** Recalcula o estado; retorna true se o marco de domínio foi atingido pela primeira vez agora. */
    boolean recompute(boolean inReviewCycle, int successfulReviews, Instant now) {
        state = MasteryCalculator.compute(attempts, recentResults, lessonsCompleted, inReviewCycle, successfulReviews);
        if (state == TopicState.MASTERED && masteryReachedAt == null) {
            masteryReachedAt = now;
            return true;
        }
        return false;
    }

    public double accuracy() {
        return attempts == 0 ? 0 : (double) correct / attempts;
    }

    public UUID getUserId() { return userId; }
    public String getTopicId() { return topicId; }
    public TopicState getState() { return state; }
    public int getLessonsCompleted() { return lessonsCompleted; }
    public int getAttempts() { return attempts; }
    public int getCorrect() { return correct; }
    public String getRecentResults() { return recentResults; }
    public Instant getLastStudiedAt() { return lastStudiedAt; }
    public Instant getMasteryReachedAt() { return masteryReachedAt; }

    public static class Key implements Serializable {
        private UUID userId;
        private String topicId;

        public Key() {
        }

        public Key(UUID userId, String topicId) {
            this.userId = userId;
            this.topicId = topicId;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && Objects.equals(userId, k.userId) && Objects.equals(topicId, k.topicId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, topicId);
        }
    }
}
