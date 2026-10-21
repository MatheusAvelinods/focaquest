package com.focaquest.questions;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** Histórico de revisões editoriais de uma questão. */
@Entity
@Table(name = "question_revision")
@IdClass(QuestionRevision.Key.class)
public class QuestionRevision {

    @Id
    @Column(name = "question_id")
    private String questionId;
    @Id
    private int revision;
    @Column(name = "content_hash")
    private String contentHash;
    @Column(name = "content_version")
    private String contentVersion;
    @Column(name = "recorded_at")
    private Instant recordedAt;

    protected QuestionRevision() {
    }

    public QuestionRevision(String questionId, int revision, String contentHash, String contentVersion, Instant recordedAt) {
        this.questionId = questionId;
        this.revision = revision;
        this.contentHash = contentHash;
        this.contentVersion = contentVersion;
        this.recordedAt = recordedAt;
    }

    public static class Key implements Serializable {
        private String questionId;
        private int revision;

        public Key() {
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && revision == k.revision && Objects.equals(questionId, k.questionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(questionId, revision);
        }
    }
}
