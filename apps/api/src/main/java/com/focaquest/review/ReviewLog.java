package com.focaquest.review;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Registro imutável de cada revisão concluída. */
@Entity
@Table(name = "review_log")
public class ReviewLog {

    @Id
    private UUID id;
    @Column(name = "review_id")
    private UUID reviewId;
    @Column(name = "session_id")
    private UUID sessionId;
    @Column(name = "reviewed_at")
    private Instant reviewedAt;
    private BigDecimal score;
    @Column(name = "previous_step")
    private int previousStep;
    @Column(name = "new_step")
    private int newStep;
    @Column(name = "new_interval_days")
    private int newIntervalDays;
    private String policy;

    protected ReviewLog() {
    }

    public ReviewLog(UUID reviewId, UUID sessionId, Instant reviewedAt, double score, int previousStep,
                     ReviewPolicy.Decision decision, String policy) {
        this.id = UUID.randomUUID();
        this.reviewId = reviewId;
        this.sessionId = sessionId;
        this.reviewedAt = reviewedAt;
        this.score = BigDecimal.valueOf(score).setScale(3, RoundingMode.HALF_UP);
        this.previousStep = previousStep;
        this.newStep = decision.step();
        this.newIntervalDays = decision.intervalDays();
        this.policy = policy;
    }

    public UUID getReviewId() { return reviewId; }
    public UUID getSessionId() { return sessionId; }
    public BigDecimal getScore() { return score; }
}
