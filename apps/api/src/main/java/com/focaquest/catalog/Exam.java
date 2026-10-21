package com.focaquest.catalog;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "exam")
public class Exam {

    @Id
    private String id;
    private String name;
    private String board;
    private int year;
    private String description;
    @Column(name = "source_ref")
    private String sourceRef;
    @Column(name = "reviewed_at")
    private LocalDate reviewedAt;

    protected Exam() {
    }

    public Exam(String id) {
        this.id = id;
    }

    public void update(String name, String board, int year, String description, String sourceRef, LocalDate reviewedAt) {
        this.name = name;
        this.board = board;
        this.year = year;
        this.description = description;
        this.sourceRef = sourceRef;
        this.reviewedAt = reviewedAt;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getBoard() { return board; }
    public int getYear() { return year; }
    public String getDescription() { return description; }
    public String getSourceRef() { return sourceRef; }
    public LocalDate getReviewedAt() { return reviewedAt; }
}
