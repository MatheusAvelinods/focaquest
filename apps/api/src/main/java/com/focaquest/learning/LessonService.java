package com.focaquest.learning;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.focaquest.catalog.CatalogService;
import com.focaquest.catalog.Subject;
import com.focaquest.catalog.Topic;
import com.focaquest.learning.LearningDtos.LessonContent;
import com.focaquest.learning.LearningDtos.LessonDto;
import com.focaquest.learning.LearningDtos.TopicRef;
import com.focaquest.shared.NotFoundException;

@Service
@Transactional(readOnly = true)
public class LessonService {

    private final LessonRepository lessons;
    private final ActivityRepository activities;
    private final CatalogService catalog;

    public LessonService(LessonRepository lessons, ActivityRepository activities, CatalogService catalog) {
        this.lessons = lessons;
        this.activities = activities;
        this.catalog = catalog;
    }

    public Lesson requirePublished(String lessonId) {
        return lessons.findById(lessonId).filter(Lesson::isPublished)
                .orElseThrow(() -> new NotFoundException("Lição não encontrada: " + lessonId));
    }

    public LessonDto get(String lessonId) {
        Lesson lesson = requirePublished(lessonId);
        return new LessonDto(content(lesson), topicRef(lesson.getTopicId()),
                activities.findByLessonIdOrderByPositionAsc(lessonId).size());
    }

    public List<String> questionIds(String lessonId) {
        return activities.findByLessonIdOrderByPositionAsc(lessonId).stream().map(Activity::getQuestionId).toList();
    }

    public LessonContent content(Lesson lesson) {
        var syllabusObjectives = catalog.objectivesOf(lesson.getTopicId());
        return new LessonContent(lesson.getId(), lesson.getTitle(), lesson.getObjective(), lesson.getExplanation(),
                lesson.getExample(), lesson.getSummary(), syllabusObjectives);
    }

    public TopicRef topicRef(String topicId) {
        Topic topic = catalog.requireTopic(topicId);
        Subject subject = catalog.requireSubject(topic.getSubjectId());
        return new TopicRef(topic.getId(), topic.getName(), subject.getId(), subject.getName());
    }
}
