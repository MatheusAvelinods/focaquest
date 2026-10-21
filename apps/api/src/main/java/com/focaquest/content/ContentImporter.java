package com.focaquest.content;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.focaquest.catalog.Exam;
import com.focaquest.catalog.ExamRepository;
import com.focaquest.catalog.ExamRole;
import com.focaquest.catalog.ExamRoleRepository;
import com.focaquest.catalog.LearningObjective;
import com.focaquest.catalog.LearningObjectiveRepository;
import com.focaquest.catalog.Subject;
import com.focaquest.catalog.SubjectRepository;
import com.focaquest.catalog.SyllabusVersion;
import com.focaquest.catalog.SyllabusVersionRepository;
import com.focaquest.catalog.Topic;
import com.focaquest.catalog.TopicRepository;
import com.focaquest.content.ContentModel.Bundle;
import com.focaquest.content.ContentModel.LessonFile;
import com.focaquest.content.ContentModel.QuestionDef;
import com.focaquest.content.ContentModel.SubjectFile;
import com.focaquest.content.ContentModel.TopicDef;
import com.focaquest.learning.Activity;
import com.focaquest.learning.ActivityRepository;
import com.focaquest.learning.Lesson;
import com.focaquest.learning.LessonRepository;
import com.focaquest.questions.Question;
import com.focaquest.questions.QuestionOption;
import com.focaquest.questions.QuestionRepository;
import com.focaquest.questions.QuestionRevision;
import com.focaquest.questions.QuestionRevisionRepository;

/**
 * Importação idempotente do conteúdo versionado. Reimportar o mesmo conteúdo não altera
 * nada; uma questão alterada ganha nova revisão; itens removidos dos arquivos são retirados
 * (status RETIRED), preservando as tentativas já registradas.
 */
@Service
public class ContentImporter {

    private static final Logger log = LoggerFactory.getLogger(ContentImporter.class);

    private final ExamRepository exams;
    private final ExamRoleRepository roles;
    private final SyllabusVersionRepository syllabi;
    private final SubjectRepository subjects;
    private final TopicRepository topics;
    private final LearningObjectiveRepository objectives;
    private final QuestionRepository questions;
    private final QuestionRevisionRepository revisions;
    private final LessonRepository lessons;
    private final ActivityRepository activities;
    private final ContentImportRepository imports;
    private final Clock clock;

    public ContentImporter(ExamRepository exams, ExamRoleRepository roles, SyllabusVersionRepository syllabi,
                           SubjectRepository subjects, TopicRepository topics, LearningObjectiveRepository objectives,
                           QuestionRepository questions, QuestionRevisionRepository revisions,
                           LessonRepository lessons, ActivityRepository activities, ContentImportRepository imports,
                           Clock clock) {
        this.exams = exams;
        this.roles = roles;
        this.syllabi = syllabi;
        this.subjects = subjects;
        this.topics = topics;
        this.objectives = objectives;
        this.questions = questions;
        this.revisions = revisions;
        this.lessons = lessons;
        this.activities = activities;
        this.imports = imports;
        this.clock = clock;
    }

    public record Result(String contentVersion, int created, int updated, int unchanged, int retired) {
    }

    @Transactional
    public Result importFrom(Path root) {
        Bundle bundle = ContentLoader.load(root);
        List<String> errors = ContentValidator.validate(bundle);
        if (!errors.isEmpty()) {
            throw new ContentValidationException(errors);
        }
        return importBundle(bundle);
    }

    @Transactional
    public Result importBundle(Bundle bundle) {
        Instant now = clock.instant();
        Counter c = new Counter();

        for (var f : bundle.exams()) {
            var e = f.value().exam();
            Exam exam = exams.findById(e.id()).orElseGet(() -> new Exam(e.id()));
            exam.update(e.name(), e.board(), e.year(), e.description(), e.sourceRef(), e.reviewedAt());
            exams.save(exam);
            for (var r : f.value().roles()) {
                ExamRole role = roles.findById(r.id()).orElseGet(() -> new ExamRole(r.id()));
                role.update(e.id(), r.name(), r.description());
                roles.save(role);
                var sd = r.syllabus();
                for (SyllabusVersion other : syllabi.findByRoleId(r.id())) {
                    if (!other.getId().equals(sd.id()) && other.isCurrent()) {
                        other.update(other.getRoleId(), other.getVersion(), false, other.getSourceRef(),
                                other.getReviewedAt());
                    }
                }
                SyllabusVersion v = syllabi.findById(sd.id()).orElseGet(() -> new SyllabusVersion(sd.id()));
                v.update(r.id(), sd.version(), true, sd.sourceRef(), sd.reviewedAt());
                syllabi.save(v);
            }
        }

        for (var f : bundle.subjects()) {
            SubjectFile s = f.value();
            Subject subject = subjects.findById(s.id()).orElseGet(() -> new Subject(s.id()));
            subject.update(s.syllabus(), s.area(), s.name(), s.description(), s.examQuestionCount(), s.position());
            subjects.save(subject);
            importTopics(s, s.topics(), null);
        }
        // Garante que tópicos existam antes de questões e lições que os referenciam.
        topics.flush();

        Set<String> questionIds = new HashSet<>();
        for (var sq : bundle.questions()) {
            QuestionDef q = sq.value();
            questionIds.add(q.id());
            var existing = questions.findById(q.id());
            Question question = existing.orElseGet(() -> new Question(q.id()));
            List<QuestionOption> options = q.options().stream()
                    .map(o -> new QuestionOption(o.label(), o.text().strip(), o.rationale().strip())).toList();
            boolean changed = question.applyContent(q.topic(), strip(q.context()), q.statement().strip(), q.correct(),
                    q.explanation().strip(), Question.Difficulty.valueOf(q.difficulty()), q.boardStyle(), q.source(),
                    q.author(), Question.Status.valueOf(q.status()), options, ContentLoader.hash(q), now);
            if (changed) {
                questions.save(question);
                revisions.save(new QuestionRevision(q.id(), question.getRevision(), question.getContentHash(),
                        bundle.contentVersion(), now));
            }
            c.count(existing.isEmpty(), changed);
        }
        questions.flush();

        Set<String> lessonIds = new HashSet<>();
        for (var sl : bundle.lessons()) {
            LessonFile l = sl.value();
            lessonIds.add(l.id());
            var existing = lessons.findById(l.id());
            Lesson lesson = existing.orElseGet(() -> new Lesson(l.id()));
            boolean changed = lesson.applyContent(l.topic(), l.title().strip(), l.objective().strip(),
                    l.explanation().strip(), l.example().strip(), strip(l.summary()),
                    l.position() == null ? 0 : l.position(), l.status(), l.reviewedAt(), ContentLoader.hash(l));
            if (changed) {
                lessons.saveAndFlush(lesson);
                activities.deleteByLessonId(l.id());
                activities.flush();
                for (int i = 0; i < l.activities().size(); i++) {
                    activities.save(new Activity(l.id(), i, l.activities().get(i)));
                }
            }
            c.count(existing.isEmpty(), changed);
        }

        for (Question q : questions.findAll()) {
            if (!questionIds.contains(q.getId()) && q.retire(now)) c.retired++;
        }
        for (Lesson l : lessons.findAll()) {
            if (!lessonIds.contains(l.getId()) && l.retire()) c.retired++;
        }

        imports.save(new ContentImport(bundle.contentVersion(), now, c.created, c.updated, c.unchanged));
        Result result = new Result(bundle.contentVersion(), c.created, c.updated, c.unchanged, c.retired);
        log.info("content.import version={} created={} updated={} unchanged={} retired={}", result.contentVersion(),
                result.created(), result.updated(), result.unchanged(), result.retired());
        return result;
    }

    private void importTopics(SubjectFile s, List<TopicDef> defs, String parentId) {
        for (int i = 0; i < defs.size(); i++) {
            TopicDef t = defs.get(i);
            Topic topic = topics.findById(t.id()).orElseGet(() -> new Topic(t.id()));
            topic.update(s.id(), parentId, t.code(), t.name(), t.description(), i, s.sourceRef(), s.reviewedAt());
            topics.save(topic);
            List<String> objs = t.objectives() == null ? List.of() : t.objectives();
            List<String> keep = new ArrayList<>();
            for (int j = 0; j < objs.size(); j++) {
                String id = t.id() + ":" + j;
                keep.add(id);
                LearningObjective o = objectives.findById(id).orElseGet(() -> new LearningObjective(id));
                o.update(t.id(), objs.get(j), j);
                objectives.save(o);
            }
            objectives.findByTopicId(t.id()).stream().filter(o -> !keep.contains(o.getId())).forEach(objectives::delete);
            if (t.children() != null) {
                topics.flush();
                importTopics(s, t.children(), t.id());
            }
        }
    }

    private static String strip(String s) {
        return s == null ? null : s.strip();
    }

    private static final class Counter {
        int created, updated, unchanged, retired;

        void count(boolean isNew, boolean changed) {
            if (isNew) created++;
            else if (changed) updated++;
            else unchanged++;
        }
    }
}
