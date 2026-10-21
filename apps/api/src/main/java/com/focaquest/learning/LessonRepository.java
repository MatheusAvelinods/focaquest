package com.focaquest.learning;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, String> {

    List<Lesson> findByTopicIdInAndStatusOrderByPositionAsc(Collection<String> topicIds, String status);

    List<Lesson> findByTopicIdAndStatusOrderByPositionAsc(String topicId, String status);
}
