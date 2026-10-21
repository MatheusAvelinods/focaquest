package com.focaquest.catalog;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SubjectRepository extends JpaRepository<Subject, String> {
    List<Subject> findBySyllabusVersionIdOrderByPositionAsc(String syllabusVersionId);
}
