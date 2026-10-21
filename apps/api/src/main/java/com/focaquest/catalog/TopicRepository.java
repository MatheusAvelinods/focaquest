package com.focaquest.catalog;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TopicRepository extends JpaRepository<Topic, String> {
    List<Topic> findBySubjectIdInOrderByPositionAsc(Collection<String> subjectIds);

    /** Cobertura editorial: quantas lições e questões publicadas existem por tópico. */
    @Query(value = """
            SELECT t.id AS topicId,
                   (SELECT count(*) FROM lesson l WHERE l.topic_id = t.id AND l.status = 'PUBLISHED') AS lessons,
                   (SELECT count(*) FROM question q WHERE q.topic_id = t.id AND q.status = 'PUBLISHED') AS questions
            FROM topic t WHERE t.subject_id IN (:subjectIds)
            """, nativeQuery = true)
    List<CoverageRow> coverage(Collection<String> subjectIds);

    @Query(value = """
            SELECT l.id AS id, l.title AS title, l.topic_id AS topicId
            FROM lesson l JOIN topic t ON t.id = l.topic_id
            WHERE t.subject_id IN (:subjectIds) AND l.status = 'PUBLISHED'
            ORDER BY l.position, l.id
            """, nativeQuery = true)
    List<LessonRow> publishedLessons(Collection<String> subjectIds);

    interface LessonRow {
        String getId();
        String getTitle();
        String getTopicId();
    }

    interface CoverageRow {
        String getTopicId();
        long getLessons();
        long getQuestions();
    }
}
