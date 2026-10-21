package com.focaquest.auth;

import java.time.Instant;
import java.util.UUID;

import com.focaquest.shared.MissionType;
import com.focaquest.shared.StudyStrategy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "selected_role_id")
    private String selectedRoleId;

    @Enumerated(EnumType.STRING)
    private StudyStrategy strategy = StudyStrategy.EDITAL_ORDER;

    @Column(name = "strategy_subject_id")
    private String strategySubjectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_mission_type")
    private MissionType preferredMissionType = MissionType.STANDARD;

    @Column(name = "focus_mode")
    private boolean focusMode;

    @Column(name = "reduce_motion")
    private boolean reduceMotion;

    @Column(name = "show_timer")
    private boolean showTimer;

    @Column(name = "streaks_enabled")
    private boolean streaksEnabled;

    @Column(name = "notifications_enabled")
    private boolean notificationsEnabled;

    protected AppUser() {
    }

    public AppUser(String email, String passwordHash, String displayName, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public Instant getCreatedAt() { return createdAt; }
    public String getSelectedRoleId() { return selectedRoleId; }
    public StudyStrategy getStrategy() { return strategy; }
    public String getStrategySubjectId() { return strategySubjectId; }
    public MissionType getPreferredMissionType() { return preferredMissionType; }
    public boolean isFocusMode() { return focusMode; }
    public boolean isReduceMotion() { return reduceMotion; }
    public boolean isShowTimer() { return showTimer; }
    public boolean isStreaksEnabled() { return streaksEnabled; }
    public boolean isNotificationsEnabled() { return notificationsEnabled; }

    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setSelectedRoleId(String selectedRoleId) { this.selectedRoleId = selectedRoleId; }
    public void setStrategy(StudyStrategy strategy) { this.strategy = strategy; }
    public void setStrategySubjectId(String strategySubjectId) { this.strategySubjectId = strategySubjectId; }
    public void setPreferredMissionType(MissionType preferredMissionType) { this.preferredMissionType = preferredMissionType; }
    public void setFocusMode(boolean focusMode) { this.focusMode = focusMode; }
    public void setReduceMotion(boolean reduceMotion) { this.reduceMotion = reduceMotion; }
    public void setShowTimer(boolean showTimer) { this.showTimer = showTimer; }
    public void setStreaksEnabled(boolean streaksEnabled) { this.streaksEnabled = streaksEnabled; }
    public void setNotificationsEnabled(boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }
}
