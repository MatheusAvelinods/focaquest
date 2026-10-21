package com.focaquest.progress;

import static com.focaquest.progress.MasteryCalculator.append;
import static com.focaquest.progress.MasteryCalculator.compute;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MasteryCalculatorTest {

    @Test
    void semEvidenciasNaoIniciado() {
        assertThat(compute(0, "", 0, false, 0)).isEqualTo(TopicState.NOT_STARTED);
    }

    @Test
    void poucasEvidenciasEmAprendizagem() {
        assertThat(compute(1, "1", 0, false, 0)).isEqualTo(TopicState.LEARNING);
        assertThat(compute(1, "0", 0, false, 0)).isEqualTo(TopicState.LEARNING);
    }

    @Test
    void errosRecentesPedemReforco() {
        assertThat(compute(3, "100", 1, true, 0)).isEqualTo(TopicState.NEEDS_REINFORCEMENT);
        assertThat(compute(2, "00", 0, false, 0)).isEqualTo(TopicState.NEEDS_REINFORCEMENT);
    }

    @Test
    void licaoConcluidaComRevisaoAgendadaEstaEmRevisao() {
        assertThat(compute(3, "111", 1, true, 0)).isEqualTo(TopicState.IN_REVIEW);
    }

    @Test
    void visualizarLicaoSemResponderNaoGeraDominio() {
        assertThat(compute(0, "", 5, true, 0)).isEqualTo(TopicState.IN_REVIEW);
        assertThat(compute(0, "", 5, true, 3)).isNotEqualTo(TopicState.MASTERED);
    }

    @Test
    void dominioExigeRevisoesEspacadasBemSucedidas() {
        assertThat(compute(9, "111111", 1, true, 1)).isEqualTo(TopicState.IN_REVIEW);
        assertThat(compute(9, "111111", 1, true, 2)).isEqualTo(TopicState.MASTERED);
        assertThat(compute(9, "110111", 1, true, 2)).isEqualTo(TopicState.MASTERED); // 5/6 ≥ 80%
        assertThat(compute(9, "100111", 1, true, 2)).isEqualTo(TopicState.IN_REVIEW); // 4/6 < 80%
    }

    @Test
    void janelaRecenteDescartaResultadosAntigos() {
        String r = "";
        for (int i = 0; i < 4; i++) r = append(r, false);
        for (int i = 0; i < 6; i++) r = append(r, true);
        assertThat(r).isEqualTo("111111");
        assertThat(MasteryCalculator.recentAccuracy(r)).isEqualTo(1.0);
    }
}
