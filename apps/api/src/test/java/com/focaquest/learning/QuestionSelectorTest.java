package com.focaquest.learning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.focaquest.learning.QuestionSelector.History;
import com.focaquest.shared.MissionType;

class QuestionSelectorTest {

    private static final Instant NOW = Instant.parse("2026-10-21T12:00:00Z");

    @Test
    void licaoUsaAtividadesNaOrdemEditorialLimitadasPeloTipo() {
        var lesson = List.of("q3", "q1", "q2");
        assertThat(QuestionSelector.forLesson(lesson, List.of("q1", "q2", "q3", "q4"), Map.of(), MissionType.MICRO, NOW))
                .containsExactly("q3", "q1");
        assertThat(QuestionSelector.forLesson(lesson, List.of("q1", "q2", "q3", "q4"), Map.of(), MissionType.STANDARD, NOW))
                .containsExactly("q3", "q1", "q2");
    }

    @Test
    void missaoAprofundadaCompletaComQuestoesDoTopico() {
        assertThat(QuestionSelector.forLesson(List.of("q1"), List.of("q1", "q2", "q3"), Map.of(), MissionType.DEEP, NOW))
                .containsExactly("q1", "q2", "q3");
    }

    @Test
    void praticaPriorizaErradasDepoisIneditasDepoisAcertadas() {
        var history = Map.of(
                "acertada", new History(NOW.minus(Duration.ofDays(5)), true),
                "errada", new History(NOW.minus(Duration.ofDays(5)), false));
        assertThat(QuestionSelector.forPractice(List.of("acertada", "inedita", "errada"), history,
                MissionType.STANDARD, NOW))
                .containsExactly("errada", "inedita", "acertada");
    }

    @Test
    void evitaRepetirQuestoesRespondidasRecentemente() {
        var history = Map.of(
                "recente", new History(NOW.minus(Duration.ofHours(1)), false),
                "antiga", new History(NOW.minus(Duration.ofDays(3)), true));
        assertThat(QuestionSelector.forPractice(List.of("recente", "antiga"), history, MissionType.MICRO, NOW))
                .containsExactly("antiga", "recente");
        assertThat(QuestionSelector.forPractice(List.of("recente", "antiga", "nova"), history, MissionType.MICRO, NOW))
                .containsExactly("nova", "antiga");
    }

    @Test
    void entreAcertadasEscolheAMenosRecente() {
        var history = Map.of(
                "a", new History(NOW.minus(Duration.ofDays(2)), true),
                "b", new History(NOW.minus(Duration.ofDays(9)), true));
        assertThat(QuestionSelector.forPractice(List.of("a", "b"), history, MissionType.MICRO, NOW))
                .containsExactly("b", "a");
    }
}
