package com.focaquest.learning;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Microlição: objetivo, explicação curta, exemplo aplicado e atividades de recuperação. */
@Entity
@Table(name = "lesson")
public class Lesson {

    public static final String PUBLISHED = "PUBLISHED";

    @Id
    private String id;
    @Column(name = "topic_id")
    private String topicId;
    private String title;
    private String objective;
    private String explanation;
    private String example;
    private String summary;
    private int position;
    private String status;
    @Column(name = "content_hash")
    private String contentHash;
    @Column(name = "reviewed_at")
    private LocalDate reviewedAt;

    protected Lesson() {
    }

    public Lesson(String id) {
        this.id = id;
    }

    public boolean applyContent(String topicId, String title, String objective, String explanation, String example,
                                String summary, int position, String status, LocalDate reviewedAt, String contentHash) {
        if (contentHash.equals(this.contentHash)) {
            return false;
        }
        this.topicId = topicId;
        this.title = title;
        this.objective = objective;
        this.explanation = explanation;
        this.example = example;
        this.summary = summary;
        this.position = position;
        this.status = status;
        this.reviewedAt = reviewedAt;
        this.contentHash = contentHash;
        return true;
    }

    public boolean retire() {
        if ("RETIRED".equals(status)) {
            return false;
        }
        status = "RETIRED";
        contentHash = "retired";
        return true;
    }

    public boolean isPublished() {
        return PUBLISHED.equals(status);
    }

    public String getId() { return id; }
    public String getTopicId() { return topicId; }
    public String getTitle() { return title; }
    public String getObjective() { return objective; }
    public String getExplanation() { return explanation; }
    public String getExample() { return example; }
    public String getSummary() { return summary; }
    public int getPosition() { return position; }
    public String getStatus() { return status; }
    public LocalDate getReviewedAt() { return reviewedAt; }
}
