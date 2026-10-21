package com.focaquest.review;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.focaquest.shared.NotFoundException;

@Service
public class ReviewService {

    private final ReviewScheduleRepository schedules;
    private final ReviewLogRepository logs;
    private final ReviewPolicy policy;
    private final Clock clock;

    public ReviewService(ReviewScheduleRepository schedules, ReviewLogRepository logs, ReviewPolicy policy, Clock clock) {
        this.schedules = schedules;
        this.logs = logs;
        this.policy = policy;
        this.clock = clock;
    }

    /** Agenda a primeira revisão de um tópico estudado. Não altera um agendamento existente. */
    @Transactional
    public ReviewSchedule ensureScheduled(UUID userId, String topicId) {
        return schedules.findByUserIdAndTopicId(userId, topicId)
                .orElseGet(() -> schedules.save(
                        new ReviewSchedule(userId, topicId, policy.initial(), policy.name(), clock.instant())));
    }

    /**
     * Registra o resultado de uma revisão e calcula o próximo intervalo. Idempotente por sessão:
     * concluir duas vezes a mesma sessão não avança a escada novamente.
     */
    @Transactional
    public ReviewOutcome complete(UUID userId, UUID reviewId, UUID sessionId, double score) {
        ReviewSchedule schedule = require(userId, reviewId);
        if (sessionId != null && logs.existsBySessionId(sessionId)) {
            return new ReviewOutcome(schedule.getId(), schedule.getTopicId(), schedule.getStep(),
                    schedule.getIntervalDays(), schedule.getDueAt(), false);
        }
        Instant now = clock.instant();
        int previous = schedule.getStep();
        ReviewPolicy.Decision decision = policy.next(previous, score);
        schedule.apply(decision, policy.name(), now);
        logs.save(new ReviewLog(schedule.getId(), sessionId, now, score, previous, decision, policy.name()));
        return new ReviewOutcome(schedule.getId(), schedule.getTopicId(), decision.step(), decision.intervalDays(),
                schedule.getDueAt(), decision.successful());
    }

    @Transactional(readOnly = true)
    public ReviewSchedule require(UUID userId, UUID reviewId) {
        return schedules.findByIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new NotFoundException("Revisão não encontrada."));
    }

    @Transactional(readOnly = true)
    public List<ReviewSchedule> all(UUID userId) {
        return schedules.findByUserIdOrderByDueAtAsc(userId);
    }

    @Transactional(readOnly = true)
    public List<ReviewSchedule> due(UUID userId) {
        Instant now = clock.instant();
        return all(userId).stream().filter(r -> r.isDue(now)).toList();
    }

    @Transactional(readOnly = true)
    public Optional<ReviewSchedule> find(UUID userId, String topicId) {
        return schedules.findByUserIdAndTopicId(userId, topicId);
    }

    public record ReviewOutcome(UUID reviewId, String topicId, int step, int intervalDays, Instant nextDueAt,
                                boolean successful) {
    }
}
