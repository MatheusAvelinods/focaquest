package com.focaquest.catalog;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "topic")
public class Topic {

    @Id
    private String id;
    @Column(name = "subject_id")
    private String subjectId;
    @Column(name = "parent_id")
    private String parentId;
    @Column(name = "edital_code")
    private String editalCode;
    private String name;
    private String description;
    private int position;
    @Column(name = "source_ref")
    private String sourceRef;
    @Column(name = "reviewed_at")
    private LocalDate reviewedAt;

    protected Topic() {
    }

    public Topic(String id) {
        this.id = id;
    }

    public void update(String subjectId, String parentId, String editalCode, String name, String description,
                       int position, String sourceRef, LocalDate reviewedAt) {
        this.subjectId = subjectId;
        this.parentId = parentId;
        this.editalCode = editalCode;
        this.name = name;
        this.description = description;
        this.position = position;
        this.sourceRef = sourceRef;
        this.reviewedAt = reviewedAt;
    }

    public String getId() { return id; }
    public String getSubjectId() { return subjectId; }
    public String getParentId() { return parentId; }
    public String getEditalCode() { return editalCode; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getPosition() { return position; }
    public String getSourceRef() { return sourceRef; }
    public LocalDate getReviewedAt() { return reviewedAt; }
}
