package com.focaquest.catalog;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LearningObjectiveRepository extends JpaRepository<LearningObjective, String> {
    List<LearningObjective> findByTopicIdInOrderByPositionAsc(Collection<String> topicIds);

    List<LearningObjective> findByTopicId(String topicId);
}
