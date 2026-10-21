package com.focaquest.learning;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.focaquest.learning.JourneyService.Journey;
import com.focaquest.learning.JourneyService.PendingReview;
import com.focaquest.learning.LearningDtos.AnswerRequest;
import com.focaquest.learning.LearningDtos.AnswerResponse;
import com.focaquest.learning.LearningDtos.CompleteReviewRequest;
import com.focaquest.learning.LearningDtos.LessonDto;
import com.focaquest.learning.LearningDtos.SessionView;
import com.focaquest.learning.LearningDtos.StartSessionRequest;
import com.focaquest.learning.LearningDtos.UpdateSessionRequest;
import com.focaquest.shared.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Aprendizagem")
public class LearningController {

    private final LessonService lessons;
    private final StudySessionService sessions;
    private final JourneyService journey;

    public LearningController(LessonService lessons, StudySessionService sessions, JourneyService journey) {
        this.lessons = lessons;
        this.sessions = sessions;
        this.journey = journey;
    }

    @GetMapping("/me/journey")
    @Operation(summary = "Tela inicial: missão recomendada, retomada, progresso e revisões")
    public Journey journey() {
        return journey.journey(CurrentUser.id());
    }

    @GetMapping("/lessons/{id}")
    @Operation(summary = "Carregar microlição")
    public LessonDto lesson(@PathVariable String id) {
        return lessons.get(id);
    }

    @PostMapping("/study-sessions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Iniciar sessão (ou retomar a mesma missão, se já estiver aberta)")
    public SessionView start(@Valid @RequestBody StartSessionRequest req) {
        return sessions.start(CurrentUser.id(), req);
    }

    @GetMapping("/study-sessions/{id}")
    @Operation(summary = "Consultar sessão; gabaritos só aparecem para questões já respondidas")
    public SessionView get(@PathVariable UUID id) {
        return sessions.get(CurrentUser.id(), id);
    }

    @PostMapping("/study-sessions/{id}/answers")
    @Operation(summary = "Registrar resposta e receber feedback comentado")
    public AnswerResponse answer(@PathVariable UUID id, @Valid @RequestBody AnswerRequest req) {
        return sessions.answer(CurrentUser.id(), id, req.questionId(), req.selectedOption());
    }

    @PatchMapping("/study-sessions/{id}")
    @Operation(summary = "Pausar, retomar, avançar de passo ou concluir a sessão")
    public SessionView update(@PathVariable UUID id, @RequestBody UpdateSessionRequest req) {
        return sessions.update(CurrentUser.id(), id, req);
    }

    @GetMapping("/me/reviews")
    @Operation(summary = "Consultar revisões programadas")
    public List<PendingReview> reviews() {
        return journey.reviews(CurrentUser.id());
    }

    @PostMapping("/me/reviews/{id}/complete")
    @Operation(summary = "Concluir revisão a partir da sessão de revisão realizada")
    public SessionView completeReview(@PathVariable UUID id, @Valid @RequestBody CompleteReviewRequest req) {
        return sessions.completeReview(CurrentUser.id(), id, req.sessionId());
    }
}
