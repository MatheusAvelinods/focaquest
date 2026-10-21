package com.focaquest.questions;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, String> {

    List<Question> findByTopicIdAndStatusOrderByIdAsc(String topicId, Question.Status status);

    List<Question> findByIdIn(Collection<String> ids);

    long countByTopicIdAndStatusAndBoardStyle(String topicId, Question.Status status, String boardStyle);
}
