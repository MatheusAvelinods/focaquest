package com.focaquest.learning;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ActivityRepository extends JpaRepository<Activity, String> {

    List<Activity> findByLessonIdOrderByPositionAsc(String lessonId);

    @Modifying
    @Query("delete from Activity a where a.lessonId = :lessonId")
    void deleteByLessonId(String lessonId);
}
