package com.focaquest.content;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "content_import")
public class ContentImport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "content_version")
    private String contentVersion;
    @Column(name = "imported_at")
    private Instant importedAt;
    @Column(name = "created_count")
    private int createdCount;
    @Column(name = "updated_count")
    private int updatedCount;
    @Column(name = "unchanged_count")
    private int unchangedCount;

    protected ContentImport() {
    }

    public ContentImport(String contentVersion, Instant importedAt, int created, int updated, int unchanged) {
        this.contentVersion = contentVersion;
        this.importedAt = importedAt;
        this.createdCount = created;
        this.updatedCount = updated;
        this.unchangedCount = unchanged;
    }
}
