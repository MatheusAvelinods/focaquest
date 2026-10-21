package com.focaquest.learning;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import com.focaquest.progress.TopicState;
import com.focaquest.shared.MissionType;
import com.focaquest.shared.StudyStrategy;

/**
 * Motor de recomendação baseado em regras explícitas (PRD 4.2). Função pura: as mesmas
 * entradas produzem sempre a mesma recomendação.
 *
 * <ol>
 *   <li>Priorizar revisões vencidas.</li>
 *   <li>Alternar: após duas revisões seguidas, oferecer conteúdo novo, se houver.</li>
 *   <li>Reforçar assuntos com respostas incorretas.</li>
 *   <li>Avançar no edital segundo a estratégia escolhida.</li>
 *   <li>Oferecer sempre uma alternativa de missão curta.</li>
 * </ol>
 */
public final class RecommendationEngine {

    static final double LOW_PERFORMANCE_BELOW = 0.7;

    private RecommendationEngine() {
    }

    public record TopicCandidate(
            String topicId,
            String topicName,
            String subjectId,
            String subjectName,
            TopicState state,
            int attempts,
            double accuracy,
            String nextLessonId,
            String nextLessonTitle,
            String firstLessonId,
            String firstLessonTitle,
            int questionCount,
            long boardQuestionCount) {
    }

    public record ReviewCandidate(UUID reviewId, String topicId, Instant dueAt) {
    }

    public record Input(
            StudyStrategy strategy,
            String strategySubjectId,
            MissionType preferredType,
            String board,
            List<TopicCandidate> topicsInEditalOrder,
            List<ReviewCandidate> reviews,
            List<SessionKind> recentCompletedKinds,
            Instant now) {
    }

    public record Recommendation(Mission primary, Mission alternative) {
        public static final Recommendation NONE = new Recommendation(null, null);
    }

    public static Recommendation recommend(Input in) {
        List<Mission> queue = queue(in);
        if (queue.isEmpty()) {
            return Recommendation.NONE;
        }
        Mission primary = queue.getFirst().as(in.preferredType());
        Mission alternative;
        if (in.preferredType() != MissionType.MICRO) {
            alternative = queue.getFirst().as(MissionType.MICRO);
        } else {
            alternative = queue.size() > 1 ? queue.get(1).as(MissionType.MICRO) : null;
        }
        return new Recommendation(primary, alternative);
    }

    static List<Mission> queue(Input in) {
        var byId = in.topicsInEditalOrder().stream()
                .collect(java.util.stream.Collectors.toMap(TopicCandidate::topicId, t -> t, (a, b) -> a));

        List<Mission> due = in.reviews().stream()
                .filter(r -> !r.dueAt().isAfter(in.now()) && byId.containsKey(r.topicId()))
                .sorted(Comparator.comparing(ReviewCandidate::dueAt))
                .map(r -> review(byId.get(r.topicId()), r, "Revisão programada: este é o momento ideal para fixar "
                        + byId.get(r.topicId()).topicName() + "."))
                .toList();

        List<Mission> ahead = in.reviews().stream()
                .filter(r -> r.dueAt().isAfter(in.now()) && byId.containsKey(r.topicId()))
                .sorted(Comparator.comparing(ReviewCandidate::dueAt))
                .map(r -> review(byId.get(r.topicId()), r, "Nenhuma revisão vencida: antecipe a revisão de "
                        + byId.get(r.topicId()).topicName() + "."))
                .toList();

        List<Mission> reinforcement = reinforcementCandidates(in).stream()
                .map(t -> new Mission(SessionKind.REINFORCEMENT, in.preferredType(), t.topicId(), t.topicName(),
                        t.subjectName(), t.firstLessonId(), t.firstLessonTitle(), null,
                        in.preferredType().estimatedMinutes(),
                        "Seus acertos recentes em " + t.topicName() + " indicam que vale reforçar este assunto."))
                .toList();

        List<Mission> fresh = newContent(in);

        boolean alternate = !due.isEmpty() && !fresh.isEmpty() && in.recentCompletedKinds().size() >= 2
                && in.recentCompletedKinds().get(0) == SessionKind.REVIEW
                && in.recentCompletedKinds().get(1) == SessionKind.REVIEW;

        List<Mission> queue = new ArrayList<>();
        if (alternate) {
            Mission next = fresh.getFirst();
            queue.add(withReason(next, "Você fez revisões nas últimas missões: hora de avançar com um conteúdo novo."));
        }
        switch (in.strategy()) {
            case LOW_PERFORMANCE -> {
                queue.addAll(reinforcement);
                queue.addAll(due);
                queue.addAll(fresh);
                queue.addAll(ahead);
            }
            case REVIEW_FIRST -> {
                queue.addAll(due);
                queue.addAll(ahead);
                queue.addAll(reinforcement);
                queue.addAll(fresh);
            }
            default -> {
                queue.addAll(due);
                queue.addAll(reinforcement);
                queue.addAll(fresh);
                queue.addAll(ahead);
            }
        }
        // Remove duplicatas mantendo a primeira ocorrência (e sua justificativa).
        var unique = new java.util.LinkedHashMap<String, Mission>();
        for (Mission m : queue) {
            unique.putIfAbsent(m.kind() + "|" + m.topicId() + "|" + m.lessonId() + "|" + m.reviewId(), m);
        }
        return List.copyOf(unique.values());
    }

    private static List<TopicCandidate> reinforcementCandidates(Input in) {
        Stream<TopicCandidate> s = in.topicsInEditalOrder().stream().filter(t -> t.questionCount() > 0);
        if (in.strategy() == StudyStrategy.LOW_PERFORMANCE) {
            s = s.filter(t -> t.state() == TopicState.NEEDS_REINFORCEMENT
                    || (t.attempts() >= 2 && t.accuracy() < LOW_PERFORMANCE_BELOW));
        } else {
            s = s.filter(t -> t.state() == TopicState.NEEDS_REINFORCEMENT);
        }
        // Menor desempenho primeiro; empate mantém a ordem do edital (sort é estável).
        return s.sorted(Comparator.comparingDouble(TopicCandidate::accuracy)).toList();
    }

    private static List<Mission> newContent(Input in) {
        List<TopicCandidate> withLesson = in.topicsInEditalOrder().stream()
                .filter(t -> t.nextLessonId() != null)
                .toList();
        List<TopicCandidate> ordered = new ArrayList<>(withLesson);
        String reason;
        switch (in.strategy()) {
            case UNSTUDIED_FIRST -> {
                ordered.sort(Comparator.comparing((TopicCandidate t) -> t.state() != TopicState.NOT_STARTED));
                reason = "Assunto ainda não estudado.";
            }
            case BY_SUBJECT -> {
                ordered.sort(Comparator.comparing((TopicCandidate t) -> !t.subjectId().equals(in.strategySubjectId())));
                reason = "Próximo assunto da disciplina escolhida.";
            }
            case BOARD_FOCUS -> {
                ordered.sort(Comparator.comparingLong(TopicCandidate::boardQuestionCount).reversed());
                reason = "Assunto com questões no estilo da banca " + (in.board() == null ? "" : in.board()) + ".";
            }
            default -> reason = "Próximo assunto na ordem do edital.";
        }
        return ordered.stream()
                .map(t -> new Mission(SessionKind.LESSON, in.preferredType(), t.topicId(), t.topicName(),
                        t.subjectName(), t.nextLessonId(), t.nextLessonTitle(), null,
                        in.preferredType().estimatedMinutes(),
                        t.state() == TopicState.NOT_STARTED || in.strategy() != StudyStrategy.UNSTUDIED_FIRST
                                ? reason : "Continue de onde parou neste assunto."))
                .toList();
    }

    private static Mission review(TopicCandidate t, ReviewCandidate r, String reason) {
        return new Mission(SessionKind.REVIEW, MissionType.STANDARD, t.topicId(), t.topicName(), t.subjectName(),
                null, null, r.reviewId(), MissionType.STANDARD.estimatedMinutes(), reason);
    }

    private static Mission withReason(Mission m, String reason) {
        return new Mission(m.kind(), m.missionType(), m.topicId(), m.topicName(), m.subjectName(), m.lessonId(),
                m.lessonTitle(), m.reviewId(), m.estimatedMinutes(), reason);
    }
}
