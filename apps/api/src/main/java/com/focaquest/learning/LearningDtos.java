package com.focaquest.learning;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.focaquest.progress.TopicState;
import com.focaquest.questions.QuestionViews.Feedback;
import com.focaquest.questions.QuestionViews.PublicQuestion;
import com.focaquest.shared.MissionType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public final class LearningDtos {

    private LearningDtos() {
    }

    public record TopicRef(String id, String name, String subjectId, String subjectName) {
    }

    public record LessonContent(String id, String title, String objective, String explanation, String example,
                                String summary, List<String> learningObjectives) {
    }

    public record LessonDto(LessonContent lesson, TopicRef topic, int activityCount) {
    }

    public record StepDto(int index, StudySession.StepType type, PublicQuestion question, Feedback feedback) {
    }

    public record SessionResult(int correct, int total, double score, int xpAwarded, TopicState topicState,
                                boolean masteryMilestone, Instant nextReviewAt) {
    }

    public record SessionView(
            UUID id,
            SessionKind kind,
            MissionType missionType,
            SessionStatus status,
            int currentStep,
            Instant startedAt,
            Instant lastActivityAt,
            TopicRef topic,
            LessonContent lesson,
            UUID reviewId,
            List<StepDto> steps,
            int answeredCount,
            int totalQuestions,
            SessionResult result) {
    }

    public record StartSessionRequest(
            @NotNull SessionKind kind,
            MissionType missionType,
            String lessonId,
            String topicId,
            UUID reviewId) {
    }

    public record AnswerRequest(
            @NotBlank String questionId,
            @NotBlank @Pattern(regexp = "[A-Ea-e]", message = "alternativa deve ser uma letra de A a E") String selectedOption) {
    }

    public record AnswerResponse(Feedback feedback, boolean alreadyAnswered, int xpEarned, TopicState topicState,
                                 int currentStep) {
    }

    public enum SessionAction { PAUSE, RESUME, COMPLETE }

    public record UpdateSessionRequest(SessionAction action, Integer currentStep) {
    }

    public record CompleteReviewRequest(@NotNull UUID sessionId) {
    }

    public record InterruptedSession(UUID sessionId, SessionKind kind, String topicId, String lessonId, UUID reviewId,
                                     String topicName, String lessonTitle,
                                     int currentStep, int totalSteps, int answered, int totalQuestions,
                                     Instant lastActivityAt) {
    }
}
