package com.focaquest.learning;

import com.focaquest.shared.MissionType;

/** Regras de XP. XP recompensa a conclusão de atividades e retomadas; não mede domínio. */
final class SessionXp {

    static final int PER_ANSWER = 2;
    static final int CORRECT_BONUS = 3;
    static final int REVIEW_BONUS = 10;
    static final int RESUME_BONUS = 5;
    static final int MASTERY_MILESTONE = 50;

    private SessionXp() {
    }

    static int forAnswer(boolean correct) {
        return PER_ANSWER + (correct ? CORRECT_BONUS : 0);
    }

    static int forCompletion(MissionType type, SessionKind kind) {
        int base = switch (type) {
            case MICRO -> 10;
            case STANDARD -> 20;
            case DEEP -> 35;
        };
        return kind == SessionKind.REVIEW ? base + REVIEW_BONUS : base;
    }
}
