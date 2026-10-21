package com.focaquest.progress;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** XP é uma métrica de engajamento, separada do domínio (que vem das evidências). */
@Entity
@Table(name = "xp_event")
public class XpEvent {

    @Id
    private UUID id;
    @Column(name = "user_id")
    private UUID userId;
    private int amount;
    private String reason;
    @Column(name = "reference_id")
    private String referenceId;
    @Column(name = "created_at")
    private Instant createdAt;

    protected XpEvent() {
    }

    public XpEvent(UUID userId, int amount, String reason, String referenceId, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.amount = amount;
        this.reason = reason;
        this.referenceId = referenceId;
        this.createdAt = createdAt;
    }

    public int getAmount() { return amount; }
    public String getReason() { return reason; }
}
