package com.focaquest.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "exam_role")
public class ExamRole {

    @Id
    private String id;
    @Column(name = "exam_id")
    private String examId;
    private String name;
    private String description;

    protected ExamRole() {
    }

    public ExamRole(String id) {
        this.id = id;
    }

    public void update(String examId, String name, String description) {
        this.examId = examId;
        this.name = name;
        this.description = description;
    }

    public String getId() { return id; }
    public String getExamId() { return examId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
}
