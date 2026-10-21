package com.focaquest.learning;

import java.util.UUID;

import com.focaquest.shared.MissionType;

/** Uma missão sugerida ao estudante, com a justificativa da recomendação. */
public record Mission(
        SessionKind kind,
        MissionType missionType,
        String topicId,
        String topicName,
        String subjectName,
        String lessonId,
        String lessonTitle,
        UUID reviewId,
        int estimatedMinutes,
        String reason) {

    public Mission as(MissionType type) {
        return new Mission(kind, type, topicId, topicName, subjectName, lessonId, lessonTitle, reviewId,
                type.estimatedMinutes(), reason);
    }
}
