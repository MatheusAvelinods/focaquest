package com.focaquest.catalog;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "syllabus_version")
public class SyllabusVersion {

    @Id
    private String id;
    @Column(name = "role_id")
    private String roleId;
    private String version;
    private boolean current;
    @Column(name = "source_ref")
    private String sourceRef;
    @Column(name = "reviewed_at")
    private LocalDate reviewedAt;

    protected SyllabusVersion() {
    }

    public SyllabusVersion(String id) {
        this.id = id;
    }

    public void update(String roleId, String version, boolean current, String sourceRef, LocalDate reviewedAt) {
        this.roleId = roleId;
        this.version = version;
        this.current = current;
        this.sourceRef = sourceRef;
        this.reviewedAt = reviewedAt;
    }

    public String getId() { return id; }
    public String getRoleId() { return roleId; }
    public String getVersion() { return version; }
    public boolean isCurrent() { return current; }
    public String getSourceRef() { return sourceRef; }
    public LocalDate getReviewedAt() { return reviewedAt; }
}
