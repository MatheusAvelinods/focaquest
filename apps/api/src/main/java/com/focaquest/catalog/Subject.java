package com.focaquest.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "subject")
public class Subject {

    @Id
    private String id;
    @Column(name = "syllabus_version_id")
    private String syllabusVersionId;
    private String area;
    private String name;
    private String description;
    @Column(name = "exam_question_count")
    private Integer examQuestionCount;
    private int position;

    protected Subject() {
    }

    public Subject(String id) {
        this.id = id;
    }

    public void update(String syllabusVersionId, String area, String name, String description,
                       Integer examQuestionCount, int position) {
        this.syllabusVersionId = syllabusVersionId;
        this.area = area;
        this.name = name;
        this.description = description;
        this.examQuestionCount = examQuestionCount;
        this.position = position;
    }

    public String getId() { return id; }
    public String getSyllabusVersionId() { return syllabusVersionId; }
    public String getArea() { return area; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Integer getExamQuestionCount() { return examQuestionCount; }
    public int getPosition() { return position; }
}
