package com.focaquest.catalog;

import java.time.LocalDate;
import java.util.List;

public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record ExamDto(String id, String name, String board, int year, String description, String sourceRef) {
        static ExamDto of(Exam e) {
            return new ExamDto(e.getId(), e.getName(), e.getBoard(), e.getYear(), e.getDescription(), e.getSourceRef());
        }
    }

    public record RoleDto(String id, String examId, String name, String description) {
        static RoleDto of(ExamRole r) {
            return new RoleDto(r.getId(), r.getExamId(), r.getName(), r.getDescription());
        }
    }

    public enum Coverage { NONE, QUESTIONS_ONLY, LESSON_ONLY, COVERED }

    public record LessonRef(String id, String title) {
    }

    public record TopicDto(
            String id,
            String subjectId,
            String parentId,
            int depth,
            String editalCode,
            String name,
            String description,
            List<String> objectives,
            List<LessonRef> lessons,
            long lessonCount,
            long questionCount,
            Coverage coverage,
            String sourceRef,
            LocalDate reviewedAt) {
    }

    public record SubjectDto(
            String id,
            String area,
            String name,
            String description,
            Integer examQuestionCount,
            List<TopicDto> topics) {
    }

    public record SyllabusDto(
            ExamDto exam,
            RoleDto role,
            String syllabusVersionId,
            String version,
            String sourceRef,
            LocalDate reviewedAt,
            List<SubjectDto> subjects) {
    }
}
