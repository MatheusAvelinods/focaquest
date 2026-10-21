package com.focaquest.review;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "review_schedule")
public class ReviewSchedule {

    @Id
    private UUID id;
    @Column(name = "user_id")
    private UUID userId;
    @Column(name = "topic_id")
    private String topicId;
    private int step;
    @Column(name = "interval_days")
    private int intervalDays;
    @Column(name = "due_at")
    private Instant dueAt;
    @Column(name = "successful_reviews")
    private int successfulReviews;
    private String policy;
    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "updated_at")
    private Instant updatedAt;

    protected ReviewSchedule() {
    }

    public ReviewSchedule(UUID userId, String topicId, ReviewPolicy.Decision initial, String policy, Instant now) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.topicId = topicId;
        this.step = initial.step();
        this.intervalDays = initial.intervalDays();
        this.dueAt = now.plus(java.time.Duration.ofDays(initial.intervalDays()));
        this.policy = policy;
        this.createdAt = now;
        this.updatedAt = now;
    }

    void apply(ReviewPolicy.Decision decision, String policy, Instant now) {
        this.step = decision.step();
        this.intervalDays = decision.intervalDays();
        this.dueAt = now.plus(java.time.Duration.ofDays(decision.intervalDays()));
        if (decision.successful()) {
            this.successfulReviews += 1;
        }
        this.policy = policy;
        this.updatedAt = now;
    }

    public boolean isDue(Instant now) {
        return !dueAt.isAfter(now);
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTopicId() { return topicId; }
    public int getStep() { return step; }
    public int getIntervalDays() { return intervalDays; }
    public Instant getDueAt() { return dueAt; }
    public int getSuccessfulReviews() { return successfulReviews; }
    public String getPolicy() { return policy; }
}
