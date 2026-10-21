package com.focaquest.content;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

import com.focaquest.content.ContentModel.Bundle;
import com.focaquest.content.ContentModel.ExamFile;
import com.focaquest.content.ContentModel.LessonFile;
import com.focaquest.content.ContentModel.QuestionDef;
import com.focaquest.content.ContentModel.QuestionFile;
import com.focaquest.content.ContentModel.Sourced;
import com.focaquest.content.ContentModel.SubjectFile;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Lê um diretório de conteúdo com a estrutura:
 * <pre>
 *   syllabus/exam.yaml        concurso, cargos e versão do edital
 *   syllabus/subjects/*.yaml  disciplinas e árvore de tópicos
 *   lessons/*.yaml            microlições
 *   questions/*.yaml          questões objetivas
 * </pre>
 */
public final class ContentLoader {

    private static final YAMLMapper YAML = YAMLMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private ContentLoader() {
    }

    public static Bundle load(Path root) {
        if (!Files.isDirectory(root)) {
            throw new ContentValidationException(List.of("Diretório de conteúdo não encontrado: " + root));
        }
        List<String> errors = new ArrayList<>();
        List<Sourced<ExamFile>> exams = read(root, root.resolve("syllabus/exam.yaml"), ExamFile.class, errors);
        List<Sourced<SubjectFile>> subjects = readAll(root, root.resolve("syllabus/subjects"), SubjectFile.class, errors);
        List<Sourced<LessonFile>> lessons = readAll(root, root.resolve("lessons"), LessonFile.class, errors);
        List<Sourced<QuestionDef>> questions = new ArrayList<>();
        for (var file : readAll(root, root.resolve("questions"), QuestionFile.class, errors)) {
            if (file.value().questions() == null) {
                errors.add(file.file() + ": lista 'questions' ausente");
                continue;
            }
            file.value().questions().forEach(q -> questions.add(new Sourced<>(file.file(), q)));
        }
        if (!errors.isEmpty()) {
            throw new ContentValidationException(errors);
        }
        return new Bundle(fingerprint(root), exams, subjects, lessons, questions);
    }

    private static <T> List<Sourced<T>> readAll(Path root, Path dir, Class<T> type, List<String> errors) {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        List<Sourced<T>> out = new ArrayList<>();
        for (Path file : yamlFiles(dir)) {
            out.addAll(read(root, file, type, errors));
        }
        return out;
    }

    private static <T> List<Sourced<T>> read(Path root, Path file, Class<T> type, List<String> errors) {
        String name = root.relativize(file).toString();
        if (!Files.isRegularFile(file)) {
            errors.add(name + ": arquivo obrigatório ausente");
            return List.of();
        }
        try {
            T value = YAML.readValue(file.toFile(), type);
            if (value == null) {
                errors.add(name + ": arquivo vazio");
                return List.of();
            }
            return List.of(new Sourced<>(name, value));
        } catch (JacksonException e) {
            errors.add(name + ": YAML inválido — " + e.getOriginalMessage());
            return List.of();
        }
    }

    private static List<Path> yamlFiles(Path dir) {
        try (Stream<Path> s = Files.walk(dir)) {
            return s.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Impressão digital do conteúdo: identifica a versão importada mesmo sem acesso ao Git. */
    private static String fingerprint(Path root) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Path file : yamlFiles(root)) {
                digest.update(root.relativize(file).toString().getBytes(StandardCharsets.UTF_8));
                digest.update(Files.readAllBytes(file));
            }
            return "sha256:" + HexFormat.of().formatHex(digest.digest()).substring(0, 16);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static String hash(Object value) {
        try {
            byte[] json = tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsBytes(value);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
