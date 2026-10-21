package com.focaquest.content;

import java.time.LocalDate;
import java.util.List;

/** Formato dos arquivos YAML versionados em {@code content/}. Ver docs/content-format.md. */
public final class ContentModel {

    private ContentModel() {
    }

    public record ExamFile(ExamDef exam, List<RoleDef> roles) {
    }

    public record ExamDef(String id, String name, String board, Integer year, String description, String sourceRef,
                          LocalDate reviewedAt) {
    }

    public record RoleDef(String id, String name, String description, SyllabusDef syllabus) {
    }

    public record SyllabusDef(String id, String version, String sourceRef, LocalDate reviewedAt) {
    }

    public record SubjectFile(String id, String syllabus, String area, String name, String description,
                              Integer examQuestionCount, Integer position, String sourceRef, LocalDate reviewedAt,
                              List<TopicDef> topics) {
    }

    public record TopicDef(String id, String code, String name, String description, List<String> objectives,
                           List<TopicDef> children) {
    }

    public record LessonFile(String id, String topic, String title, String status, Integer position,
                             LocalDate reviewedAt, String objective, String explanation, String example,
                             String summary, List<String> activities) {
    }

    public record QuestionFile(List<QuestionDef> questions) {
    }

    public record QuestionDef(String id, String topic, String difficulty, String boardStyle, String source,
                              String author, String status, String context, String statement,
                              List<OptionDef> options, String correct, String explanation) {
    }

    public record OptionDef(String label, String text, String rationale) {
    }

    /** Conteúdo completo carregado de um diretório, com a origem de cada item para mensagens de erro. */
    public record Bundle(String contentVersion, List<Sourced<ExamFile>> exams, List<Sourced<SubjectFile>> subjects,
                         List<Sourced<LessonFile>> lessons, List<Sourced<QuestionDef>> questions) {
    }

    public record Sourced<T>(String file, T value) {
    }
}
