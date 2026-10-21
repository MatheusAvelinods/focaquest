package com.focaquest.progress;

import java.time.Instant;
import java.util.List;

public final class ProgressDtos {

    private ProgressDtos() {
    }

    public record TopicProgressDto(String topicId, String subjectId, String name, TopicState state, int attempts,
                                   int correct, double accuracy, Instant lastStudiedAt, Instant masteryReachedAt) {
    }

    public record SubjectProgressDto(String subjectId, String name, String area, int topicsTotal, int topicsStarted,
                                     int topicsMastered, int attempts, int correct, double accuracy) {
    }

    public record EditalProgress(int topicsTotal, int topicsStarted, int topicsMastered, double coveragePercent) {
    }

    public record QuestionStats(int attempts, int correct, double accuracy) {
    }

    public record ReviewStats(long due, long scheduled) {
    }

    public record ProgressSummary(long xp, EditalProgress edital, QuestionStats questions, ReviewStats reviews,
                                  List<SubjectProgressDto> subjects, List<TopicProgressDto> topics) {
    }

    public record StateChange(String topicId, TopicState previous, TopicState current, boolean masteryMilestone) {
    }
}
