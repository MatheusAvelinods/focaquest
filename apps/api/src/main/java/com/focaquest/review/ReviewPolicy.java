package com.focaquest.review;

/**
 * Política de espaçamento de revisões. Implementações devem ser determinísticas: o mesmo
 * passo e a mesma pontuação sempre produzem a mesma decisão.
 */
public interface ReviewPolicy {

    String name();

    /** Intervalo (em dias) da primeira revisão após o estudo inicial do tópico. */
    Decision initial();

    /**
     * @param currentStep passo atual do tópico na escada de revisões
     * @param score       proporção de acertos na revisão, entre 0 e 1
     */
    Decision next(int currentStep, double score);

    record Decision(int step, int intervalDays, boolean successful) {
    }
}
