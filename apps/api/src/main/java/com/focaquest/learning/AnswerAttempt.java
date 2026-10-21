package com.focaquest.learning;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Evidência de aprendizagem: uma resposta a uma questão dentro de uma sessão. */
@Entity
@Table(name = "answer_attempt")
public class AnswerAttempt {

    @Id
    private UUID id;
    @Column(name = "session_id")
    private UUID sessionId;
    @Column(name = "user_id")
    private UUID userId;
    @Column(name = "question_id")
    private String questionId;
    @Column(name = "question_revision")
    private int questionRevision;
    @Column(name = "topic_id")
    private String topicId;
    @Column(name = "selected_option", length = 1)
    private String selectedOption;
    private boolean correct;
    @Column(name = "answered_at")
    private Instant answeredAt;

    protected AnswerAttempt() {
    }

    public AnswerAttempt(UUID sessionId, UUID userId, String questionId, int questionRevision, String topicId,
                         String selectedOption, boolean correct, Instant answeredAt) {
        this.id = UUID.randomUUID();
        this.sessionId = sessionId;
        this.userId = userId;
        this.questionId = questionId;
        this.questionRevision = questionRevision;
        this.topicId = topicId;
        this.selectedOption = selectedOption;
        this.correct = correct;
        this.answeredAt = answeredAt;
    }

    public UUID getId() { return id; }
    public UUID getSessionId() { return sessionId; }
    public String getQuestionId() { return questionId; }
    public String getTopicId() { return topicId; }
    public String getSelectedOption() { return selectedOption; }
    public boolean isCorrect() { return correct; }
    public Instant getAnsweredAt() { return answeredAt; }
}
