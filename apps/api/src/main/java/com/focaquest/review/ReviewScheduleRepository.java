package com.focaquest.review;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewScheduleRepository extends JpaRepository<ReviewSchedule, UUID> {

    Optional<ReviewSchedule> findByUserIdAndTopicId(UUID userId, String topicId);

    Optional<ReviewSchedule> findByIdAndUserId(UUID id, UUID userId);

    List<ReviewSchedule> findByUserIdOrderByDueAtAsc(UUID userId);

    long countByUserIdAndDueAtLessThanEqual(UUID userId, Instant now);
}
