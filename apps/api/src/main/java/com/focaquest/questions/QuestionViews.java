package com.focaquest.questions;

import java.util.List;

/**
 * Projeções de questões enviadas ao cliente. {@link PublicQuestion} nunca contém gabarito ou
 * explicação: esses dados só aparecem em {@link Feedback}, depois que a resposta foi registrada.
 */
public final class QuestionViews {

    private QuestionViews() {
    }

    public record PublicOption(String label, String text) {
    }

    public record PublicQuestion(String id, String topicId, String context, String statement,
                                 Question.Difficulty difficulty, String source, List<PublicOption> options) {

        public static PublicQuestion of(Question q) {
            return new PublicQuestion(q.getId(), q.getTopicId(), q.getContext(), q.getStatement(), q.getDifficulty(),
                    q.getSource(), q.sortedOptions().stream().map(o -> new PublicOption(o.getLabel(), o.getText())).toList());
        }
    }

    public record FeedbackOption(String label, String text, String rationale, boolean correct) {
    }

    public record Feedback(String questionId, String selectedOption, String correctOption, boolean correct,
                           String explanation, List<FeedbackOption> options) {

        public static Feedback of(Question q, String selected) {
            return new Feedback(q.getId(), selected, q.getCorrectOption(), q.isCorrect(selected), q.getExplanation(),
                    q.sortedOptions().stream()
                            .map(o -> new FeedbackOption(o.getLabel(), o.getText(), o.getRationale(),
                                    q.isCorrect(o.getLabel())))
                            .toList());
        }
    }
}
