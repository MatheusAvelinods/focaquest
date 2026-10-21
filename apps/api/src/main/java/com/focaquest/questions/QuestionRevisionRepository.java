package com.focaquest.questions;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRevisionRepository extends JpaRepository<QuestionRevision, QuestionRevision.Key> {
}
