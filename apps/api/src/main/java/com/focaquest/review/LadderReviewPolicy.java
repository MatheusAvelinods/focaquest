package com.focaquest.review;

import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Escada de intervalos fixos (ladder-v1).
 * <ul>
 *   <li>pontuação &ge; 80%: sobe um degrau (intervalo maior) e conta como revisão bem-sucedida;</li>
 *   <li>entre 50% e 80%: permanece no mesmo degrau;</li>
 *   <li>abaixo de 50%: volta ao primeiro degrau.</li>
 * </ul>
 */
@Component
public class LadderReviewPolicy implements ReviewPolicy {

    public static final String NAME = "ladder-v1";
    static final List<Integer> INTERVALS = List.of(1, 3, 7, 14, 30, 60);
    static final double SUCCESS_THRESHOLD = 0.8;
    static final double KEEP_THRESHOLD = 0.5;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Decision initial() {
        return new Decision(0, INTERVALS.getFirst(), false);
    }

    @Override
    public Decision next(int currentStep, double score) {
        if (score < 0 || score > 1) {
            throw new IllegalArgumentException("score deve estar entre 0 e 1");
        }
        int step = Math.clamp(currentStep, 0, INTERVALS.size() - 1);
        if (score >= SUCCESS_THRESHOLD) {
            int next = Math.min(step + 1, INTERVALS.size() - 1);
            return new Decision(next, INTERVALS.get(next), true);
        }
        if (score >= KEEP_THRESHOLD) {
            return new Decision(step, INTERVALS.get(step), false);
        }
        return new Decision(0, INTERVALS.getFirst(), false);
    }
}
