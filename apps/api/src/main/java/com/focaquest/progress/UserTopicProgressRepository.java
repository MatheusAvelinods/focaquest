package com.focaquest.progress;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTopicProgressRepository extends JpaRepository<UserTopicProgress, UserTopicProgress.Key> {

    List<UserTopicProgress> findByUserId(UUID userId);
}
