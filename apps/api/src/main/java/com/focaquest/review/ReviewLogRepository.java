package com.focaquest.review;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewLogRepository extends JpaRepository<ReviewLog, UUID> {

    boolean existsBySessionId(UUID sessionId);
}
