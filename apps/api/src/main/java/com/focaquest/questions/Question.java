package com.focaquest.questions;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "question")
public class Question {

    public enum Status { DRAFT, PUBLISHED, RETIRED }

    public enum Difficulty { FACIL, MEDIA, DIFICIL }

    @Id
    private String id;
    @Column(name = "topic_id", nullable = false)
    private String topicId;
    private String context;
    @Column(nullable = false)
    private String statement;
    @Column(name = "correct_option", nullable = false, length = 1)
    private String correctOption;
    @Column(nullable = false)
    private String explanation;
    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;
    @Column(name = "board_style")
    private String boardStyle;
    private String source;
    private String author;
    @Enumerated(EnumType.STRING)
    private Status status;
    private int revision;
    @Column(name = "content_hash")
    private String contentHash;
    @Column(name = "updated_at")
    private Instant updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "question_option", joinColumns = @JoinColumn(name = "question_id"))
    private List<QuestionOption> options = new ArrayList<>();

    protected Question() {
    }

    public Question(String id) {
        this.id = id;
        this.revision = 0;
    }

    /** Aplica uma nova versão editorial. Retorna true quando o conteúdo mudou (nova revisão). */
    public boolean applyContent(String topicId, String context, String statement, String correctOption,
                                String explanation, Difficulty difficulty, String boardStyle, String source,
                                String author, Status status, List<QuestionOption> options, String contentHash,
                                Instant now) {
        if (contentHash.equals(this.contentHash)) {
            return false;
        }
        this.topicId = topicId;
        this.context = context;
        this.statement = statement;
        this.correctOption = correctOption;
        this.explanation = explanation;
        this.difficulty = difficulty;
        this.boardStyle = boardStyle;
        this.source = source;
        this.author = author;
        this.status = status;
        this.options.clear();
        this.options.addAll(options);
        this.contentHash = contentHash;
        this.revision += 1;
        this.updatedAt = now;
        return true;
    }

    /** Retira a questão de circulação sem apagar o histórico de tentativas. */
    public boolean retire(Instant now) {
        if (status == Status.RETIRED) {
            return false;
        }
        status = Status.RETIRED;
        contentHash = "retired";
        updatedAt = now;
        return true;
    }

    public boolean isCorrect(String label) {
        return correctOption.equalsIgnoreCase(label);
    }

    public boolean hasOption(String label) {
        return options.stream().anyMatch(o -> o.getLabel().equalsIgnoreCase(label));
    }

    public List<QuestionOption> sortedOptions() {
        return options.stream().sorted(Comparator.comparing(QuestionOption::getLabel)).toList();
    }

    public String getId() { return id; }
    public String getTopicId() { return topicId; }
    public String getContext() { return context; }
    public String getStatement() { return statement; }
    public String getCorrectOption() { return correctOption; }
    public String getExplanation() { return explanation; }
    public Difficulty getDifficulty() { return difficulty; }
    public String getBoardStyle() { return boardStyle; }
    public String getSource() { return source; }
    public String getAuthor() { return author; }
    public Status getStatus() { return status; }
    public int getRevision() { return revision; }
    public String getContentHash() { return contentHash; }
}
