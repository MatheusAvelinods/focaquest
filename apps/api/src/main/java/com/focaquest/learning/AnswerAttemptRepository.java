package com.focaquest.learning;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerAttemptRepository extends JpaRepository<AnswerAttempt, UUID> {

    List<AnswerAttempt> findBySessionId(UUID sessionId);

    Optional<AnswerAttempt> findBySessionIdAndQuestionId(UUID sessionId, String questionId);

    List<AnswerAttempt> findByUserIdAndTopicIdOrderByAnsweredAtDesc(UUID userId, String topicId);
}
