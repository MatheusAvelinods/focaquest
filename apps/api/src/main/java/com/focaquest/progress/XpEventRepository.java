package com.focaquest.progress;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface XpEventRepository extends JpaRepository<XpEvent, UUID> {

    @Query("select coalesce(sum(e.amount), 0) from XpEvent e where e.userId = :userId")
    long totalFor(UUID userId);

    boolean existsByUserIdAndReasonAndReferenceId(UUID userId, String reason, String referenceId);
}
