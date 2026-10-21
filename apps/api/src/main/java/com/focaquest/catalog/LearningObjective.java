package com.focaquest.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "learning_objective")
public class LearningObjective {

    @Id
    private String id;
    @Column(name = "topic_id")
    private String topicId;
    private String description;
    private int position;

    protected LearningObjective() {
    }

    public LearningObjective(String id) {
        this.id = id;
    }

    public void update(String topicId, String description, int position) {
        this.topicId = topicId;
        this.description = description;
        this.position = position;
    }

    public String getId() { return id; }
    public String getTopicId() { return topicId; }
    public String getDescription() { return description; }
    public int getPosition() { return position; }
}
