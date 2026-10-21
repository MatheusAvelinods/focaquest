package com.focaquest.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LadderReviewPolicyTest {

    private final LadderReviewPolicy policy = new LadderReviewPolicy();

    @Test
    void primeiraRevisaoEmUmDia() {
        assertThat(policy.initial()).isEqualTo(new ReviewPolicy.Decision(0, 1, false));
    }

    @ParameterizedTest(name = "passo {0}, pontuação {1} → passo {2}, {3} dias, sucesso={4}")
    @CsvSource({
            "0, 1.0,  1, 3,  true",
            "1, 0.8,  2, 7,  true",
            "2, 1.0,  3, 14, true",
            "5, 1.0,  5, 60, true",   // topo da escada: permanece no maior intervalo
            "3, 0.66, 3, 14, false",  // desempenho intermediário: mantém o degrau
            "3, 0.5,  3, 14, false",
            "4, 0.49, 0, 1,  false",  // desempenho baixo: volta ao início
            "2, 0.0,  0, 1,  false",
    })
    void decideProximoIntervalo(int step, double score, int newStep, int days, boolean successful) {
        assertThat(policy.next(step, score)).isEqualTo(new ReviewPolicy.Decision(newStep, days, successful));
    }

    @Test
    void ehDeterministica() {
        assertThat(policy.next(2, 0.75)).isEqualTo(policy.next(2, 0.75));
    }

    @Test
    void rejeitaPontuacaoForaDoIntervalo() {
        assertThatThrownBy(() -> policy.next(0, 1.5)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void passoInvalidoEhLimitadoAEscada() {
        assertThat(policy.next(99, 1.0).step()).isEqualTo(LadderReviewPolicy.INTERVALS.size() - 1);
        assertThat(policy.next(-3, 0.6).step()).isZero();
    }
}
