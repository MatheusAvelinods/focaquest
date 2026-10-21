package com.focaquest.shared;

/** Duração indicativa de uma missão. A duração nunca é uma obrigação. */
public enum MissionType {
    MICRO(4, 2),
    STANDARD(10, 4),
    DEEP(20, 8);

    private final int estimatedMinutes;
    private final int maxQuestions;

    MissionType(int estimatedMinutes, int maxQuestions) {
        this.estimatedMinutes = estimatedMinutes;
        this.maxQuestions = maxQuestions;
    }

    public int estimatedMinutes() {
        return estimatedMinutes;
    }

    public int maxQuestions() {
        return maxQuestions;
    }
}
