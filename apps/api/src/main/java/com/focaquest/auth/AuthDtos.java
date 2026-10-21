package com.focaquest.auth;

import java.util.UUID;

import com.focaquest.shared.MissionType;
import com.focaquest.shared.StudyStrategy;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 100, message = "a senha deve ter entre 8 e 100 caracteres") String password,
            @NotBlank @Size(max = 100) String displayName) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record MeResponse(
            UUID id,
            String email,
            String displayName,
            String selectedRoleId,
            Preferences preferences) {

        static MeResponse of(AppUser u) {
            return new MeResponse(u.getId(), u.getEmail(), u.getDisplayName(), u.getSelectedRoleId(), Preferences.of(u));
        }
    }

    public record Preferences(
            StudyStrategy strategy,
            String strategySubjectId,
            MissionType preferredMissionType,
            boolean focusMode,
            boolean reduceMotion,
            boolean showTimer,
            boolean streaksEnabled,
            boolean notificationsEnabled) {

        static Preferences of(AppUser u) {
            return new Preferences(u.getStrategy(), u.getStrategySubjectId(), u.getPreferredMissionType(),
                    u.isFocusMode(), u.isReduceMotion(), u.isShowTimer(), u.isStreaksEnabled(),
                    u.isNotificationsEnabled());
        }
    }

    /** Atualização parcial: campos nulos são mantidos. */
    public record UpdateProfileRequest(
            @Size(max = 100) String displayName,
            String selectedRoleId,
            StudyStrategy strategy,
            String strategySubjectId,
            MissionType preferredMissionType,
            Boolean focusMode,
            Boolean reduceMotion,
            Boolean showTimer,
            Boolean streaksEnabled,
            Boolean notificationsEnabled) {
    }
}
