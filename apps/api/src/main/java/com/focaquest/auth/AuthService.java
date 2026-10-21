package com.focaquest.auth;

import java.time.Clock;
import java.util.UUID;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.focaquest.auth.AuthDtos.RegisterRequest;
import com.focaquest.auth.AuthDtos.UpdateProfileRequest;
import com.focaquest.catalog.CatalogService;
import com.focaquest.shared.ConflictException;
import com.focaquest.shared.NotFoundException;
import com.focaquest.shared.StudyStrategy;

@Service
public class AuthService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final CatalogService catalog;
    private final Clock clock;
    // Usado quando o e-mail não existe, para que o tempo de resposta não revele contas cadastradas.
    private final String dummyHash;

    public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder, CatalogService catalog, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.catalog = catalog;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("focaquest-dummy-password");
    }

    @Transactional
    public AppUser register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Já existe uma conta com este e-mail.");
        }
        AppUser user = new AppUser(email, passwordEncoder.encode(req.password()), req.displayName().trim(), clock.instant());
        return users.save(user);
    }

    @Transactional(readOnly = true)
    public AppUser authenticate(String email, String password) {
        var user = users.findByEmailIgnoreCase(email.trim());
        String hash = user.map(AppUser::getPasswordHash).orElse(dummyHash);
        boolean matches = passwordEncoder.matches(password, hash);
        if (user.isEmpty() || !matches) {
            throw new BadCredentialsException("E-mail ou senha incorretos.");
        }
        return user.get();
    }

    @Transactional(readOnly = true)
    public AppUser get(UUID id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
    }

    @Transactional
    public AppUser updateProfile(UUID id, UpdateProfileRequest req) {
        AppUser u = get(id);
        if (req.displayName() != null && !req.displayName().isBlank()) u.setDisplayName(req.displayName().trim());
        if (req.selectedRoleId() != null) {
            catalog.requireRole(req.selectedRoleId());
            u.setSelectedRoleId(req.selectedRoleId());
        }
        if (req.strategy() != null) u.setStrategy(req.strategy());
        if (req.strategySubjectId() != null) {
            if (req.strategySubjectId().isBlank()) {
                u.setStrategySubjectId(null);
            } else {
                catalog.requireSubject(req.strategySubjectId());
                u.setStrategySubjectId(req.strategySubjectId());
            }
        }
        if (u.getStrategy() == StudyStrategy.BY_SUBJECT && u.getStrategySubjectId() == null) {
            throw new IllegalArgumentException("A estratégia por disciplina exige uma disciplina selecionada.");
        }
        if (req.preferredMissionType() != null) u.setPreferredMissionType(req.preferredMissionType());
        if (req.focusMode() != null) u.setFocusMode(req.focusMode());
        if (req.reduceMotion() != null) u.setReduceMotion(req.reduceMotion());
        if (req.showTimer() != null) u.setShowTimer(req.showTimer());
        if (req.streaksEnabled() != null) u.setStreaksEnabled(req.streaksEnabled());
        if (req.notificationsEnabled() != null) u.setNotificationsEnabled(req.notificationsEnabled());
        return u;
    }
}
