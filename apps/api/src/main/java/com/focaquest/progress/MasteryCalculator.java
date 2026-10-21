package com.focaquest.progress;

/**
 * Calcula o estado de um tópico a partir de evidências de aprendizagem — respostas e
 * revisões —, e não de lições apenas visualizadas.
 */
public final class MasteryCalculator {

    /** Quantidade de respostas recentes consideradas. */
    public static final int RECENT_WINDOW = 6;
    static final double REINFORCEMENT_BELOW = 0.6;
    static final double MASTERY_FROM = 0.8;
    static final int MASTERY_MIN_ATTEMPTS = 5;
    static final int MASTERY_MIN_SUCCESSFUL_REVIEWS = 2;

    private MasteryCalculator() {
    }

    /**
     * @param attempts          total de respostas no tópico
     * @param recentResults     últimos resultados, do mais antigo ao mais recente ('1' acerto, '0' erro)
     * @param lessonsCompleted  missões de estudo concluídas no tópico
     * @param inReviewCycle     se o tópico já tem revisão agendada
     * @param successfulReviews revisões espaçadas com bom desempenho
     */
    public static TopicState compute(int attempts, String recentResults, int lessonsCompleted,
                                     boolean inReviewCycle, int successfulReviews) {
        if (attempts == 0 && lessonsCompleted == 0) {
            return TopicState.NOT_STARTED;
        }
        double recent = recentAccuracy(recentResults);
        int recentCount = recentResults.length();
        if (recentCount >= 2 && recent < REINFORCEMENT_BELOW) {
            return TopicState.NEEDS_REINFORCEMENT;
        }
        if (attempts >= MASTERY_MIN_ATTEMPTS && recent >= MASTERY_FROM
                && successfulReviews >= MASTERY_MIN_SUCCESSFUL_REVIEWS) {
            return TopicState.MASTERED;
        }
        if (inReviewCycle && lessonsCompleted > 0) {
            return TopicState.IN_REVIEW;
        }
        return TopicState.LEARNING;
    }

    public static double recentAccuracy(String recentResults) {
        if (recentResults.isEmpty()) {
            return 0;
        }
        long hits = recentResults.chars().filter(c -> c == '1').count();
        return (double) hits / recentResults.length();
    }

    /** Acrescenta um resultado à janela recente, descartando os mais antigos. */
    public static String append(String recentResults, boolean correct) {
        String next = recentResults + (correct ? '1' : '0');
        return next.length() > RECENT_WINDOW ? next.substring(next.length() - RECENT_WINDOW) : next;
    }
}
