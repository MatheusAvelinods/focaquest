package com.focaquest.learning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.focaquest.learning.RecommendationEngine.Input;
import com.focaquest.learning.RecommendationEngine.ReviewCandidate;
import com.focaquest.learning.RecommendationEngine.TopicCandidate;
import com.focaquest.progress.TopicState;
import com.focaquest.shared.MissionType;
import com.focaquest.shared.StudyStrategy;

class RecommendationEngineTest {

    private static final Instant NOW = Instant.parse("2026-10-21T12:00:00Z");

    private static TopicCandidate topic(String id, String subject, TopicState state, double accuracy, boolean hasNewLesson) {
        return new TopicCandidate(id, "Tópico " + id, subject, "Disciplina " + subject, state,
                state == TopicState.NOT_STARTED ? 0 : 5, accuracy,
                hasNewLesson ? "l." + id : null, hasNewLesson ? "Lição " + id : null,
                "l." + id, "Lição " + id, 3, id.equals("t3") ? 3 : 0);
    }

    private static Input input(StudyStrategy strategy, MissionType type, List<TopicCandidate> topics,
                               List<ReviewCandidate> reviews, List<SessionKind> recent) {
        return new Input(strategy, "s2", type, "FGV", topics, reviews, recent, NOW);
    }

    private final List<TopicCandidate> fresh = List.of(
            topic("t1", "s1", TopicState.NOT_STARTED, 0, true),
            topic("t2", "s1", TopicState.NOT_STARTED, 0, true),
            topic("t3", "s2", TopicState.NOT_STARTED, 0, true));

    @Test
    void semHistoricoSegueAOrdemDoEdital() {
        var rec = RecommendationEngine.recommend(input(StudyStrategy.EDITAL_ORDER, MissionType.STANDARD, fresh,
                List.of(), List.of()));
        assertThat(rec.primary().kind()).isEqualTo(SessionKind.LESSON);
        assertThat(rec.primary().lessonId()).isEqualTo("l.t1");
        assertThat(rec.primary().missionType()).isEqualTo(MissionType.STANDARD);
    }

    @Test
    void sempreOfereceAlternativaCurta() {
        var rec = RecommendationEngine.recommend(input(StudyStrategy.EDITAL_ORDER, MissionType.DEEP, fresh,
                List.of(), List.of()));
        assertThat(rec.alternative()).isNotNull();
        assertThat(rec.alternative().missionType()).isEqualTo(MissionType.MICRO);
        assertThat(rec.alternative().estimatedMinutes()).isEqualTo(MissionType.MICRO.estimatedMinutes());
    }

    @Test
    void quandoPreferenciaEhMicroAlternativaEhOutraMissao() {
        var rec = RecommendationEngine.recommend(input(StudyStrategy.EDITAL_ORDER, MissionType.MICRO, fresh,
                List.of(), List.of()));
        assertThat(rec.primary().lessonId()).isEqualTo("l.t1");
        assertThat(rec.alternative().lessonId()).isEqualTo("l.t2");
    }

    @Test
    void revisaoVencidaTemPrioridade() {
        var reviewId = UUID.randomUUID();
        var topics = List.of(topic("t1", "s1", TopicState.IN_REVIEW, 1, false), fresh.get(1));
        var rec = RecommendationEngine.recommend(input(StudyStrategy.EDITAL_ORDER, MissionType.STANDARD, topics,
                List.of(new ReviewCandidate(reviewId, "t1", NOW.minus(Duration.ofHours(1)))), List.of()));
        assertThat(rec.primary().kind()).isEqualTo(SessionKind.REVIEW);
        assertThat(rec.primary().reviewId()).isEqualTo(reviewId);
    }

    @Test
    void revisaoFuturaNaoPassaNaFrenteDeConteudoNovo() {
        var topics = List.of(topic("t1", "s1", TopicState.IN_REVIEW, 1, false), fresh.get(1));
        var rec = RecommendationEngine.recommend(input(StudyStrategy.EDITAL_ORDER, MissionType.STANDARD, topics,
                List.of(new ReviewCandidate(UUID.randomUUID(), "t1", NOW.plus(Duration.ofDays(2)))), List.of()));
        assertThat(rec.primary().kind()).isEqualTo(SessionKind.LESSON);
        assertThat(rec.primary().lessonId()).isEqualTo("l.t2");
    }

    @Test
    void aposDuasRevisoesSeguidasAlternaParaConteudoNovo() {
        var topics = List.of(topic("t1", "s1", TopicState.IN_REVIEW, 1, false), fresh.get(1));
        var reviews = List.of(new ReviewCandidate(UUID.randomUUID(), "t1", NOW.minus(Duration.ofHours(1))));
        var rec = RecommendationEngine.recommend(input(StudyStrategy.EDITAL_ORDER, MissionType.STANDARD, topics,
                reviews, List.of(SessionKind.REVIEW, SessionKind.REVIEW)));
        assertThat(rec.primary().kind()).isEqualTo(SessionKind.LESSON);
        assertThat(rec.primary().reason()).contains("revisões");
    }

    @Test
    void reforcaAssuntoComErros() {
        var topics = List.of(topic("t1", "s1", TopicState.NEEDS_REINFORCEMENT, 0.3, false), fresh.get(1));
        var rec = RecommendationEngine.recommend(input(StudyStrategy.EDITAL_ORDER, MissionType.STANDARD, topics,
                List.of(), List.of()));
        assertThat(rec.primary().kind()).isEqualTo(SessionKind.REINFORCEMENT);
        assertThat(rec.primary().topicId()).isEqualTo("t1");
    }

    @Test
    void estrategiaPorDisciplinaPriorizaDisciplinaEscolhida() {
        var rec = RecommendationEngine.recommend(input(StudyStrategy.BY_SUBJECT, MissionType.STANDARD, fresh,
                List.of(), List.of()));
        assertThat(rec.primary().topicId()).isEqualTo("t3");
    }

    @Test
    void estrategiaBancaPriorizaQuestoesNoEstiloDaBanca() {
        var rec = RecommendationEngine.recommend(input(StudyStrategy.BOARD_FOCUS, MissionType.STANDARD, fresh,
                List.of(), List.of()));
        assertThat(rec.primary().topicId()).isEqualTo("t3");
        assertThat(rec.primary().reason()).contains("FGV");
    }

    @Test
    void estrategiaNaoEstudadosPrimeiro() {
        var topics = List.of(topic("t1", "s1", TopicState.LEARNING, 0.9, true), fresh.get(1));
        var rec = RecommendationEngine.recommend(input(StudyStrategy.UNSTUDIED_FIRST, MissionType.STANDARD, topics,
                List.of(), List.of()));
        assertThat(rec.primary().topicId()).isEqualTo("t2");
    }

    @Test
    void estrategiaBaixoDesempenhoPriorizaReforcoSobreRevisao() {
        var topics = List.of(topic("t1", "s1", TopicState.IN_REVIEW, 1, false),
                topic("t2", "s1", TopicState.IN_REVIEW, 0.5, false));
        var reviews = List.of(new ReviewCandidate(UUID.randomUUID(), "t1", NOW.minus(Duration.ofHours(1))));
        var rec = RecommendationEngine.recommend(input(StudyStrategy.LOW_PERFORMANCE, MissionType.STANDARD, topics,
                reviews, List.of()));
        assertThat(rec.primary().kind()).isEqualTo(SessionKind.REINFORCEMENT);
        assertThat(rec.primary().topicId()).isEqualTo("t2");
    }

    @Test
    void semConteudoNovoAntecipaRevisao() {
        var topics = List.of(topic("t1", "s1", TopicState.IN_REVIEW, 1, false));
        var rec = RecommendationEngine.recommend(input(StudyStrategy.EDITAL_ORDER, MissionType.STANDARD, topics,
                List.of(new ReviewCandidate(UUID.randomUUID(), "t1", NOW.plus(Duration.ofDays(3)))), List.of()));
        assertThat(rec.primary().kind()).isEqualTo(SessionKind.REVIEW);
        assertThat(rec.primary().reason()).contains("antecipe");
    }

    @Test
    void semNadaParaEstudarRetornaVazio() {
        var rec = RecommendationEngine.recommend(input(StudyStrategy.EDITAL_ORDER, MissionType.STANDARD, List.of(),
                List.of(), List.of()));
        assertThat(rec.primary()).isNull();
        assertThat(rec.alternative()).isNull();
    }
}
