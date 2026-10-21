package com.focaquest.learning;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.focaquest.shared.MissionType;

/**
 * Escolhe as questões de uma missão de forma determinística, evitando repetir em excesso
 * questões respondidas recentemente (PRD 4.2, regra 4).
 */
public final class QuestionSelector {

    /** Questões respondidas dentro desta janela só são reutilizadas se faltarem alternativas. */
    static final Duration RECENT = Duration.ofHours(20);

    private QuestionSelector() {
    }

    public record History(Instant lastAnsweredAt, boolean lastCorrect) {
    }

    /**
     * Missão de lição: as atividades da própria lição vêm primeiro, na ordem editorial; em
     * missões aprofundadas, completa com outras questões do tópico.
     */
    public static List<String> forLesson(List<String> lessonQuestions, List<String> topicQuestions,
                                         Map<String, History> history, MissionType type, Instant now) {
        Set<String> out = new LinkedHashSet<>();
        for (String q : lessonQuestions) {
            if (out.size() >= type.maxQuestions()) break;
            out.add(q);
        }
        if (type == MissionType.DEEP) {
            List<String> extra = rank(topicQuestions.stream().filter(q -> !out.contains(q)).toList(), history, now);
            for (String q : extra) {
                if (out.size() >= type.maxQuestions()) break;
                out.add(q);
            }
        }
        return List.copyOf(out);
    }

    /** Reforço e revisão: questões erradas e inéditas primeiro, as menos recentes depois. */
    public static List<String> forPractice(List<String> topicQuestions, Map<String, History> history,
                                           MissionType type, Instant now) {
        List<String> ranked = rank(topicQuestions, history, now);
        return List.copyOf(ranked.subList(0, Math.min(type.maxQuestions(), ranked.size())));
    }

    static List<String> rank(List<String> questions, Map<String, History> history, Instant now) {
        Instant recentCutoff = now.minus(RECENT);
        Comparator<String> order = Comparator
                .comparing((String q) -> isRecent(history.get(q), recentCutoff))
                .thenComparingInt(q -> group(history.get(q)))
                .thenComparing(q -> history.get(q) == null ? Instant.MIN : history.get(q).lastAnsweredAt())
                .thenComparing(Comparator.naturalOrder());
        List<String> sorted = new ArrayList<>(questions);
        sorted.sort(order);
        return sorted;
    }

    private static boolean isRecent(History h, Instant cutoff) {
        return h != null && h.lastAnsweredAt().isAfter(cutoff);
    }

    /** 0 = errada na última tentativa, 1 = inédita, 2 = acertada na última tentativa. */
    private static int group(History h) {
        if (h == null) return 1;
        return h.lastCorrect() ? 2 : 0;
    }
}
