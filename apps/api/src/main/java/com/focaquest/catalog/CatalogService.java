package com.focaquest.catalog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.focaquest.catalog.CatalogDtos.Coverage;
import com.focaquest.catalog.CatalogDtos.ExamDto;
import com.focaquest.catalog.CatalogDtos.LessonRef;
import com.focaquest.catalog.CatalogDtos.RoleDto;
import com.focaquest.catalog.CatalogDtos.SubjectDto;
import com.focaquest.catalog.CatalogDtos.SyllabusDto;
import com.focaquest.catalog.CatalogDtos.TopicDto;
import com.focaquest.shared.NotFoundException;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final ExamRepository exams;
    private final ExamRoleRepository roles;
    private final SyllabusVersionRepository syllabusVersions;
    private final SubjectRepository subjects;
    private final TopicRepository topics;
    private final LearningObjectiveRepository objectives;

    public CatalogService(ExamRepository exams, ExamRoleRepository roles, SyllabusVersionRepository syllabusVersions,
                          SubjectRepository subjects, TopicRepository topics, LearningObjectiveRepository objectives) {
        this.exams = exams;
        this.roles = roles;
        this.syllabusVersions = syllabusVersions;
        this.subjects = subjects;
        this.topics = topics;
        this.objectives = objectives;
    }

    public List<ExamDto> listExams() {
        return exams.findAllByOrderByYearDescNameAsc().stream().map(ExamDto::of).toList();
    }

    public List<RoleDto> listRoles(String examId) {
        if (!exams.existsById(examId)) {
            throw new NotFoundException("Concurso não encontrado: " + examId);
        }
        return roles.findByExamIdOrderByNameAsc(examId).stream().map(RoleDto::of).toList();
    }

    public ExamRole requireRole(String roleId) {
        return roles.findById(roleId).orElseThrow(() -> new NotFoundException("Cargo não encontrado: " + roleId));
    }

    public Subject requireSubject(String subjectId) {
        return subjects.findById(subjectId)
                .orElseThrow(() -> new NotFoundException("Disciplina não encontrada: " + subjectId));
    }

    public Topic requireTopic(String topicId) {
        return topics.findById(topicId).orElseThrow(() -> new NotFoundException("Tópico não encontrado: " + topicId));
    }

    public List<String> objectivesOf(String topicId) {
        return objectives.findByTopicIdInOrderByPositionAsc(List.of(topicId)).stream()
                .map(LearningObjective::getDescription).toList();
    }

    public SyllabusDto syllabus(String roleId) {
        ExamRole role = requireRole(roleId);
        Exam exam = exams.findById(role.getExamId()).orElseThrow();
        SyllabusVersion version = currentVersion(roleId);
        List<Subject> subjectList = subjects.findBySyllabusVersionIdOrderByPositionAsc(version.getId());
        List<String> subjectIds = subjectList.stream().map(Subject::getId).toList();
        List<Topic> topicList = subjectIds.isEmpty() ? List.of() : topics.findBySubjectIdInOrderByPositionAsc(subjectIds);

        Map<String, List<String>> objectivesByTopic = topicList.isEmpty() ? Map.of()
                : objectives.findByTopicIdInOrderByPositionAsc(topicList.stream().map(Topic::getId).toList()).stream()
                        .collect(Collectors.groupingBy(LearningObjective::getTopicId, LinkedHashMap::new,
                                Collectors.mapping(LearningObjective::getDescription, Collectors.toList())));
        Map<String, TopicRepository.CoverageRow> coverage = subjectIds.isEmpty() ? Map.of()
                : topics.coverage(subjectIds).stream()
                        .collect(Collectors.toMap(TopicRepository.CoverageRow::getTopicId, r -> r));

        Map<String, List<LessonRef>> lessonsByTopic = subjectIds.isEmpty() ? Map.of()
                : topics.publishedLessons(subjectIds).stream()
                        .collect(Collectors.groupingBy(TopicRepository.LessonRow::getTopicId, LinkedHashMap::new,
                                Collectors.mapping(r -> new LessonRef(r.getId(), r.getTitle()), Collectors.toList())));

        Map<String, List<Topic>> bySubject = topicList.stream()
                .collect(Collectors.groupingBy(Topic::getSubjectId));

        List<SubjectDto> subjectDtos = new ArrayList<>();
        for (Subject s : subjectList) {
            List<TopicDto> ordered = new ArrayList<>();
            flatten(bySubject.getOrDefault(s.getId(), List.of()), objectivesByTopic, lessonsByTopic, coverage, ordered);
            subjectDtos.add(new SubjectDto(s.getId(), s.getArea(), s.getName(), s.getDescription(),
                    s.getExamQuestionCount(), ordered));
        }
        return new SyllabusDto(ExamDto.of(exam), RoleDto.of(role), version.getId(), version.getVersion(),
                version.getSourceRef(), version.getReviewedAt(), subjectDtos);
    }

    /** Tópicos do cargo na ordem do edital (disciplina, depois hierarquia e posição). */
    public List<TopicDto> topicsInEditalOrder(String roleId) {
        return syllabus(roleId).subjects().stream().flatMap(s -> s.topics().stream()).toList();
    }

    private SyllabusVersion currentVersion(String roleId) {
        return syllabusVersions.findFirstByRoleIdAndCurrentTrue(roleId)
                .orElseThrow(() -> new NotFoundException("Cargo sem edital publicado: " + roleId));
    }

    /** Percorre a árvore em profundidade preservando a ordem do edital. */
    private static void flatten(List<Topic> topics, Map<String, List<String>> objectives,
                                Map<String, List<LessonRef>> lessons,
                                Map<String, TopicRepository.CoverageRow> coverage, List<TopicDto> out) {
        Map<String, List<Topic>> children = new HashMap<>();
        List<Topic> roots = new ArrayList<>();
        for (Topic t : topics) {
            if (t.getParentId() == null) {
                roots.add(t);
            } else {
                children.computeIfAbsent(t.getParentId(), k -> new ArrayList<>()).add(t);
            }
        }
        Comparator<Topic> byPosition = Comparator.comparingInt(Topic::getPosition);
        roots.sort(byPosition);
        children.values().forEach(l -> l.sort(byPosition));
        for (Topic root : roots) {
            visit(root, 0, children, objectives, lessons, coverage, out);
        }
    }

    private static void visit(Topic t, int depth, Map<String, List<Topic>> children,
                              Map<String, List<String>> objectives, Map<String, List<LessonRef>> lessons,
                              Map<String, TopicRepository.CoverageRow> coverage, List<TopicDto> out) {
        var row = coverage.get(t.getId());
        long lessonCount = row == null ? 0 : row.getLessons();
        long questions = row == null ? 0 : row.getQuestions();
        Coverage cov = lessonCount > 0 && questions > 0 ? Coverage.COVERED
                : lessonCount > 0 ? Coverage.LESSON_ONLY
                : questions > 0 ? Coverage.QUESTIONS_ONLY
                : Coverage.NONE;
        out.add(new TopicDto(t.getId(), t.getSubjectId(), t.getParentId(), depth, t.getEditalCode(), t.getName(),
                t.getDescription(), objectives.getOrDefault(t.getId(), List.of()),
                lessons.getOrDefault(t.getId(), List.of()), lessonCount, questions, cov,
                t.getSourceRef(), t.getReviewedAt()));
        for (Topic child : children.getOrDefault(t.getId(), List.of())) {
            visit(child, depth + 1, children, objectives, lessons, coverage, out);
        }
    }
}
