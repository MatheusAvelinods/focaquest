package com.focaquest.learning;

public enum SessionKind {
    /** Estudo de uma microlição nova. */
    LESSON,
    /** Retomada de um tópico com baixo desempenho, priorizando questões erradas ou inéditas. */
    REINFORCEMENT,
    /** Revisão espaçada programada: apenas recuperação, sem reapresentar a lição. */
    REVIEW
}
