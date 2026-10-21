package com.focaquest.content;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.focaquest.content.ContentModel.Bundle;
import com.focaquest.content.ContentModel.LessonFile;
import com.focaquest.content.ContentModel.OptionDef;
import com.focaquest.content.ContentModel.QuestionDef;
import com.focaquest.content.ContentModel.Sourced;
import com.focaquest.content.ContentModel.TopicDef;

/**
 * Valida o pacote de conteúdo inteiro antes de qualquer escrita no banco. Um único erro
 * rejeita a importação: questões incompletas e referências quebradas nunca chegam ao estudante.
 */
public final class ContentValidator {

    static final Set<String> AREAS = Set.of("GERAIS", "ESPECIFICOS");
    static final Set<String> DIFFICULTIES = Set.of("FACIL", "MEDIA", "DIFICIL");
    static final Set<String> QUESTION_STATUSES = Set.of("DRAFT", "PUBLISHED", "RETIRED");
    static final Set<String> LESSON_STATUSES = Set.of("DRAFT", "PUBLISHED", "RETIRED");
    static final List<String> FGV_LABELS = List.of("A", "B", "C", "D", "E");
    static final String FGV = "FGV";

    private ContentValidator() {
    }

    public static List<String> validate(Bundle b) {
        List<String> errors = new ArrayList<>();
        Set<String> roleIds = new HashSet<>();
        Set<String> syllabusIds = new HashSet<>();
        Set<String> examIds = new HashSet<>();

        for (var f : b.exams()) {
            var e = f.value().exam();
            if (e == null) {
                errors.add(f.file() + ": bloco 'exam' ausente");
                continue;
            }
            requireText(errors, f.file(), "exam.id", e.id());
            requireText(errors, f.file(), "exam.name", e.name());
            requireText(errors, f.file(), "exam.board", e.board());
            requireText(errors, f.file(), "exam.sourceRef", e.sourceRef());
            if (e.year() == null) errors.add(f.file() + ": exam.year é obrigatório");
            if (!examIds.add(e.id())) errors.add(f.file() + ": concurso duplicado " + e.id());
            if (f.value().roles() == null || f.value().roles().isEmpty()) {
                errors.add(f.file() + ": o concurso precisa de pelo menos um cargo");
                continue;
            }
            for (var r : f.value().roles()) {
                requireText(errors, f.file(), "role.id", r.id());
                requireText(errors, f.file(), "role.name", r.name());
                if (!roleIds.add(r.id())) errors.add(f.file() + ": cargo duplicado " + r.id());
                if (r.syllabus() == null) {
                    errors.add(f.file() + ": cargo " + r.id() + " sem edital (syllabus)");
                } else {
                    requireText(errors, f.file(), "syllabus.id", r.syllabus().id());
                    requireText(errors, f.file(), "syllabus.version", r.syllabus().version());
                    if (!syllabusIds.add(r.syllabus().id())) {
                        errors.add(f.file() + ": edital duplicado " + r.syllabus().id());
                    }
                }
            }
        }

        Set<String> subjectIds = new HashSet<>();
        Set<String> topicIds = new HashSet<>();
        for (var f : b.subjects()) {
            var s = f.value();
            requireText(errors, f.file(), "id", s.id());
            requireText(errors, f.file(), "name", s.name());
            if (!subjectIds.add(s.id())) errors.add(f.file() + ": disciplina duplicada " + s.id());
            if (!syllabusIds.contains(s.syllabus())) {
                errors.add(f.file() + ": edital inexistente '" + s.syllabus() + "'");
            }
            if (s.area() == null || !AREAS.contains(s.area())) {
                errors.add(f.file() + ": area deve ser uma de " + AREAS);
            }
            if (s.position() == null) errors.add(f.file() + ": position é obrigatório");
            if (s.topics() == null || s.topics().isEmpty()) {
                errors.add(f.file() + ": a disciplina precisa de pelo menos um tópico");
            } else {
                validateTopics(errors, f.file(), s.topics(), topicIds);
            }
        }

        Map<String, QuestionDef> questionsById = new HashMap<>();
        for (Sourced<QuestionDef> sq : b.questions()) {
            validateQuestion(errors, sq.file(), sq.value(), topicIds);
            if (sq.value().id() != null && questionsById.putIfAbsent(sq.value().id(), sq.value()) != null) {
                errors.add(sq.file() + ": questão duplicada " + sq.value().id());
            }
        }

        Set<String> lessonIds = new HashSet<>();
        for (Sourced<LessonFile> sl : b.lessons()) {
            LessonFile l = sl.value();
            String where = sl.file() + " [" + l.id() + "]";
            requireText(errors, sl.file(), "id", l.id());
            if (!lessonIds.add(l.id())) errors.add(where + ": lição duplicada");
            if (!topicIds.contains(l.topic())) errors.add(where + ": tópico inexistente '" + l.topic() + "'");
            requireText(errors, where, "title", l.title());
            requireText(errors, where, "objective", l.objective());
            requireText(errors, where, "explanation", l.explanation());
            requireText(errors, where, "example", l.example());
            if (l.status() == null || !LESSON_STATUSES.contains(l.status())) {
                errors.add(where + ": status deve ser um de " + LESSON_STATUSES);
            }
            if (l.activities() == null || l.activities().isEmpty()) {
                errors.add(where + ": a lição precisa de pelo menos uma atividade de recuperação");
                continue;
            }
            Set<String> seen = new HashSet<>();
            for (String qid : l.activities()) {
                QuestionDef q = questionsById.get(qid);
                if (q == null) {
                    errors.add(where + ": atividade referencia questão inexistente '" + qid + "'");
                    continue;
                }
                if (!seen.add(qid)) errors.add(where + ": questão repetida nas atividades '" + qid + "'");
                if (q.topic() != null && !q.topic().equals(l.topic())) {
                    errors.add(where + ": questão '" + qid + "' pertence a outro tópico (" + q.topic() + ")");
                }
                if ("PUBLISHED".equals(l.status()) && !"PUBLISHED".equals(q.status())) {
                    errors.add(where + ": lição publicada usa questão não publicada '" + qid + "'");
                }
            }
        }
        return errors;
    }

    private static void validateTopics(List<String> errors, String file, List<TopicDef> topics, Set<String> ids) {
        for (TopicDef t : topics) {
            requireText(errors, file, "topic.id", t.id());
            requireText(errors, file, "topic.name (" + t.id() + ")", t.name());
            if (t.id() != null && !ids.add(t.id())) errors.add(file + ": tópico duplicado " + t.id());
            if (t.children() != null) validateTopics(errors, file, t.children(), ids);
        }
    }

    static void validateQuestion(List<String> errors, String file, QuestionDef q, Set<String> topicIds) {
        String where = file + " [" + q.id() + "]";
        requireText(errors, file, "question.id", q.id());
        if (!topicIds.contains(q.topic())) errors.add(where + ": tópico inexistente '" + q.topic() + "'");
        requireText(errors, where, "statement", q.statement());
        requireText(errors, where, "explanation", q.explanation());
        requireText(errors, where, "source", q.source());
        requireText(errors, where, "author", q.author());
        if (q.difficulty() == null || !DIFFICULTIES.contains(q.difficulty())) {
            errors.add(where + ": difficulty deve ser uma de " + DIFFICULTIES);
        }
        if (q.status() == null || !QUESTION_STATUSES.contains(q.status())) {
            errors.add(where + ": status deve ser um de " + QUESTION_STATUSES);
        }
        List<OptionDef> options = q.options() == null ? List.of() : q.options();
        List<String> labels = options.stream().map(OptionDef::label).toList();
        if (FGV.equals(q.boardStyle())) {
            if (!labels.equals(FGV_LABELS)) {
                errors.add(where + ": questões no formato FGV exigem exatamente as alternativas A, B, C, D e E, em ordem");
            }
        } else if (options.size() < 2) {
            errors.add(where + ": a questão precisa de pelo menos duas alternativas");
        }
        if (new HashSet<>(labels).size() != labels.size()) {
            errors.add(where + ": rótulos de alternativas repetidos");
        }
        for (OptionDef o : options) {
            if (o.label() == null || !o.label().matches("[A-E]")) {
                errors.add(where + ": rótulo de alternativa inválido '" + o.label() + "'");
            }
            if (isBlank(o.text())) errors.add(where + ": alternativa " + o.label() + " sem texto");
            if (isBlank(o.rationale())) {
                errors.add(where + ": alternativa " + o.label() + " sem comentário (rationale)");
            }
        }
        if (q.correct() == null || !labels.contains(q.correct())) {
            errors.add(where + ": gabarito '" + q.correct() + "' não corresponde a nenhuma alternativa");
        }
        long distinctTexts = options.stream().map(o -> o.text() == null ? "" : o.text().trim().toLowerCase())
                .distinct().count();
        if (distinctTexts != options.size()) {
            errors.add(where + ": há alternativas com texto idêntico");
        }
    }

    private static void requireText(List<String> errors, String where, String field, String value) {
        if (isBlank(value)) {
            errors.add(where + ": campo obrigatório '" + field + "' ausente");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
