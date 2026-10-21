package com.focaquest.learning;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Atividade de recuperação de uma lição. No MVP, sempre uma questão objetiva. */
@Entity
@Table(name = "activity")
public class Activity {

    public static final String RETRIEVAL_QUESTION = "RETRIEVAL_QUESTION";

    @Id
    private String id;
    @Column(name = "lesson_id")
    private String lessonId;
    private int position;
    private String type;
    @Column(name = "question_id")
    private String questionId;

    protected Activity() {
    }

    public Activity(String lessonId, int position, String questionId) {
        this.id = lessonId + "#" + position;
        this.lessonId = lessonId;
        this.position = position;
        this.type = RETRIEVAL_QUESTION;
        this.questionId = questionId;
    }

    public String getId() { return id; }
    public String getLessonId() { return lessonId; }
    public int getPosition() { return position; }
    public String getType() { return type; }
    public String getQuestionId() { return questionId; }
}
