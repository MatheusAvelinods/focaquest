package com.focaquest.progress;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.focaquest.auth.AuthService;
import com.focaquest.progress.ProgressDtos.ProgressSummary;
import com.focaquest.shared.BusinessRuleException;
import com.focaquest.shared.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Progresso")
public class ProgressController {

    private final ProgressService progress;
    private final AuthService auth;

    public ProgressController(ProgressService progress, AuthService auth) {
        this.progress = progress;
        this.auth = auth;
    }

    @GetMapping("/progress")
    @Operation(summary = "Consultar progresso no edital, desempenho em questões e XP")
    public ProgressSummary progress() {
        var user = auth.get(CurrentUser.id());
        if (user.getSelectedRoleId() == null) {
            throw new BusinessRuleException("Selecione um concurso e cargo para acompanhar o progresso.");
        }
        return progress.summary(user.getId(), user.getSelectedRoleId());
    }
}
