package com.focaquest.learning;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StudySessionRepository extends JpaRepository<StudySession, UUID> {

    Optional<StudySession> findByIdAndUserId(UUID id, UUID userId);

    Optional<StudySession> findFirstByUserIdAndStatusInOrderByLastActivityAtDesc(UUID userId,
                                                                                Collection<SessionStatus> statuses);

    List<StudySession> findByUserIdAndStatusOrderByCompletedAtDesc(UUID userId, SessionStatus status);

    List<StudySession> findByUserIdAndStatusIn(UUID userId, Collection<SessionStatus> statuses);

    boolean existsByUserIdAndLessonIdAndStatus(UUID userId, String lessonId, SessionStatus status);
}
