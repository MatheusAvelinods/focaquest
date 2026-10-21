package com.focaquest.learning;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.focaquest.shared.BusinessRuleException;
import com.focaquest.shared.MissionType;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/**
 * Uma missão em andamento. As questões são sorteadas e congeladas na criação, o que permite
 * interromper e retomar a sessão sem refazer questões já respondidas.
 */
@Entity
@Table(name = "study_session")
public class StudySession {

    @Id
    private UUID id;
    @Column(name = "user_id")
    private UUID userId;
    @Enumerated(EnumType.STRING)
    private SessionKind kind;
    @Enumerated(EnumType.STRING)
    @Column(name = "mission_type")
    private MissionType missionType;
    @Column(name = "topic_id")
    private String topicId;
    @Column(name = "lesson_id")
    private String lessonId;
    @Column(name = "review_id")
    private UUID reviewId;
    @Enumerated(EnumType.STRING)
    private SessionStatus status;
    @Column(name = "current_step")
    private int currentStep;
    @Column(name = "started_at")
    private Instant startedAt;
    @Column(name = "last_activity_at")
    private Instant lastActivityAt;
    @Column(name = "completed_at")
    private Instant completedAt;
    @Column(name = "xp_awarded")
    private int xpAwarded;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "session_question", joinColumns = @JoinColumn(name = "session_id"))
    @OrderColumn(name = "position")
    @Column(name = "question_id")
    private List<String> questionIds = new ArrayList<>();

    protected StudySession() {
    }

    public StudySession(UUID userId, SessionKind kind, MissionType missionType, String topicId, String lessonId,
                        UUID reviewId, List<String> questionIds, Instant now) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.kind = kind;
        this.missionType = missionType;
        this.topicId = topicId;
        this.lessonId = lessonId;
        this.reviewId = reviewId;
        this.status = SessionStatus.IN_PROGRESS;
        this.questionIds.addAll(questionIds);
        this.startedAt = now;
        this.lastActivityAt = now;
    }

    /** Passos da missão, na ordem em que são apresentados. */
    public List<StepType> steps() {
        List<StepType> steps = new ArrayList<>();
        if (presentsLesson()) {
            steps.add(StepType.OBJECTIVE);
            steps.add(StepType.EXPLANATION);
            steps.add(StepType.EXAMPLE);
        }
        questionIds.forEach(q -> steps.add(StepType.QUESTION));
        steps.add(StepType.SUMMARY);
        return steps;
    }

    /** Índice do passo de uma questão da sessão. */
    public int stepOf(String questionId) {
        int idx = questionIds.indexOf(questionId);
        if (idx < 0) {
            throw new BusinessRuleException("A questão não pertence a esta missão.");
        }
        return (presentsLesson() ? 3 : 0) + idx;
    }

    /** Revisões são apenas recuperação; lições e reforços reapresentam o conteúdo. */
    public boolean presentsLesson() {
        return kind != SessionKind.REVIEW && lessonId != null;
    }

    public void moveTo(int step, Instant now) {
        requireOpen();
        int max = steps().size() - 1;
        if (step < 0 || step > max) {
            throw new IllegalArgumentException("Passo inválido: " + step);
        }
        this.currentStep = step;
        touch(now);
    }

    public void pause(Instant now) {
        requireOpen();
        this.status = SessionStatus.PAUSED;
        this.lastActivityAt = now;
    }

    public void resume(Instant now) {
        requireOpen();
        this.status = SessionStatus.IN_PROGRESS;
        this.lastActivityAt = now;
    }

    public void complete(int xp, Instant now) {
        requireOpen();
        this.status = SessionStatus.COMPLETED;
        this.currentStep = steps().size() - 1;
        this.completedAt = now;
        this.lastActivityAt = now;
        this.xpAwarded = xp;
    }

    public void touch(Instant now) {
        this.lastActivityAt = now;
        if (status == SessionStatus.PAUSED) {
            status = SessionStatus.IN_PROGRESS;
        }
    }

    public boolean isOpen() {
        return status != SessionStatus.COMPLETED;
    }

    private void requireOpen() {
        if (!isOpen()) {
            throw new BusinessRuleException("Esta missão já foi concluída.");
        }
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public SessionKind getKind() { return kind; }
    public MissionType getMissionType() { return missionType; }
    public String getTopicId() { return topicId; }
    public String getLessonId() { return lessonId; }
    public UUID getReviewId() { return reviewId; }
    public SessionStatus getStatus() { return status; }
    public int getCurrentStep() { return currentStep; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getLastActivityAt() { return lastActivityAt; }
    public Instant getCompletedAt() { return completedAt; }
    public int getXpAwarded() { return xpAwarded; }
    public List<String> getQuestionIds() { return List.copyOf(questionIds); }

    public enum StepType { OBJECTIVE, EXPLANATION, EXAMPLE, QUESTION, SUMMARY }
}
