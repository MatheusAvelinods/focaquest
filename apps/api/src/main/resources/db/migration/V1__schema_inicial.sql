-- =====================================================================
-- FocaQuest — schema inicial
-- Conteúdo curricular usa identificadores estáveis (texto) definidos nos
-- arquivos versionados em content/. Dados de usuário usam UUID.
-- =====================================================================

-- ---------- Catálogo curricular ----------------------------------------

CREATE TABLE exam (
    id              VARCHAR(100) PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    board           VARCHAR(50)  NOT NULL,
    year            INTEGER      NOT NULL,
    description     TEXT,
    source_ref      TEXT,
    reviewed_at     DATE
);

CREATE TABLE exam_role (
    id              VARCHAR(100) PRIMARY KEY,
    exam_id         VARCHAR(100) NOT NULL REFERENCES exam (id),
    name            VARCHAR(255) NOT NULL,
    description     TEXT
);

CREATE TABLE syllabus_version (
    id              VARCHAR(100) PRIMARY KEY,
    role_id         VARCHAR(100) NOT NULL REFERENCES exam_role (id),
    version         VARCHAR(50)  NOT NULL,
    current         BOOLEAN      NOT NULL DEFAULT TRUE,
    source_ref      TEXT,
    reviewed_at     DATE,
    UNIQUE (role_id, version)
);

CREATE TABLE subject (
    id                  VARCHAR(100) PRIMARY KEY,
    syllabus_version_id VARCHAR(100) NOT NULL REFERENCES syllabus_version (id),
    area                VARCHAR(30)  NOT NULL,      -- GERAIS | ESPECIFICOS
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    exam_question_count INTEGER,
    position            INTEGER      NOT NULL
);

CREATE TABLE topic (
    id              VARCHAR(100) PRIMARY KEY,
    subject_id      VARCHAR(100) NOT NULL REFERENCES subject (id),
    parent_id       VARCHAR(100) REFERENCES topic (id),
    edital_code     VARCHAR(30),
    name            VARCHAR(500) NOT NULL,
    description     TEXT,
    position        INTEGER      NOT NULL,
    source_ref      TEXT,
    reviewed_at     DATE
);
CREATE INDEX idx_topic_subject ON topic (subject_id);

CREATE TABLE learning_objective (
    id              VARCHAR(120) PRIMARY KEY,
    topic_id        VARCHAR(100) NOT NULL REFERENCES topic (id),
    description     TEXT         NOT NULL,
    position        INTEGER      NOT NULL
);

-- ---------- Conteúdo: lições e questões --------------------------------

CREATE TABLE question (
    id              VARCHAR(120) PRIMARY KEY,
    topic_id        VARCHAR(100) NOT NULL REFERENCES topic (id),
    context         TEXT,
    statement       TEXT         NOT NULL,
    correct_option  VARCHAR(1)    NOT NULL,
    explanation     TEXT         NOT NULL,
    difficulty      VARCHAR(20)  NOT NULL,      -- FACIL | MEDIA | DIFICIL
    board_style     VARCHAR(50),
    source          TEXT         NOT NULL,
    author          VARCHAR(255) NOT NULL,
    status          VARCHAR(20)  NOT NULL,      -- DRAFT | PUBLISHED | RETIRED
    revision        INTEGER      NOT NULL DEFAULT 1,
    content_hash    VARCHAR(64)  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_question_topic ON question (topic_id);

CREATE TABLE question_option (
    question_id     VARCHAR(120) NOT NULL REFERENCES question (id) ON DELETE CASCADE,
    label           VARCHAR(1)    NOT NULL,
    text            TEXT         NOT NULL,
    rationale       TEXT         NOT NULL,
    PRIMARY KEY (question_id, label)
);

CREATE TABLE question_revision (
    question_id     VARCHAR(120) NOT NULL REFERENCES question (id),
    revision        INTEGER      NOT NULL,
    content_hash    VARCHAR(64)  NOT NULL,
    content_version VARCHAR(100),
    recorded_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (question_id, revision)
);

CREATE TABLE lesson (
    id                  VARCHAR(120) PRIMARY KEY,
    topic_id            VARCHAR(100) NOT NULL REFERENCES topic (id),
    title               VARCHAR(255) NOT NULL,
    objective           TEXT         NOT NULL,
    explanation         TEXT         NOT NULL,
    example             TEXT         NOT NULL,
    summary             TEXT,
    position            INTEGER      NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    content_hash        VARCHAR(64)  NOT NULL,
    reviewed_at         DATE
);
CREATE INDEX idx_lesson_topic ON lesson (topic_id);

CREATE TABLE activity (
    id              VARCHAR(140) PRIMARY KEY,
    lesson_id       VARCHAR(120) NOT NULL REFERENCES lesson (id) ON DELETE CASCADE,
    position        INTEGER      NOT NULL,
    type            VARCHAR(30)  NOT NULL,      -- RETRIEVAL_QUESTION
    question_id     VARCHAR(120) NOT NULL REFERENCES question (id),
    UNIQUE (lesson_id, position)
);

CREATE TABLE content_import (
    id              BIGSERIAL    PRIMARY KEY,
    content_version VARCHAR(100) NOT NULL,
    imported_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_count   INTEGER      NOT NULL,
    updated_count   INTEGER      NOT NULL,
    unchanged_count INTEGER      NOT NULL
);

-- ---------- Usuários ---------------------------------------------------

CREATE TABLE app_user (
    id                      UUID         PRIMARY KEY,
    email                   VARCHAR(255) NOT NULL UNIQUE,
    password_hash           VARCHAR(255) NOT NULL,
    display_name            VARCHAR(100) NOT NULL,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    selected_role_id        VARCHAR(100) REFERENCES exam_role (id),
    strategy                VARCHAR(30)  NOT NULL DEFAULT 'EDITAL_ORDER',
    strategy_subject_id     VARCHAR(100) REFERENCES subject (id),
    preferred_mission_type  VARCHAR(20)  NOT NULL DEFAULT 'STANDARD',
    focus_mode              BOOLEAN      NOT NULL DEFAULT FALSE,
    reduce_motion           BOOLEAN      NOT NULL DEFAULT FALSE,
    show_timer              BOOLEAN      NOT NULL DEFAULT FALSE,
    streaks_enabled         BOOLEAN      NOT NULL DEFAULT FALSE,
    notifications_enabled   BOOLEAN      NOT NULL DEFAULT FALSE
);

-- ---------- Sessões de estudo e tentativas ----------------------------

CREATE TABLE study_session (
    id                  UUID         PRIMARY KEY,
    user_id             UUID         NOT NULL REFERENCES app_user (id),
    kind                VARCHAR(20)  NOT NULL,  -- LESSON | REINFORCEMENT | REVIEW
    mission_type        VARCHAR(20)  NOT NULL,  -- MICRO | STANDARD | DEEP
    topic_id            VARCHAR(100) NOT NULL REFERENCES topic (id),
    lesson_id           VARCHAR(120) REFERENCES lesson (id),
    review_id           UUID,
    status              VARCHAR(20)  NOT NULL,  -- IN_PROGRESS | PAUSED | COMPLETED
    current_step        INTEGER      NOT NULL DEFAULT 0,
    started_at          TIMESTAMPTZ  NOT NULL,
    last_activity_at    TIMESTAMPTZ  NOT NULL,
    completed_at        TIMESTAMPTZ,
    xp_awarded          INTEGER      NOT NULL DEFAULT 0
);
CREATE INDEX idx_session_user_status ON study_session (user_id, status);

CREATE TABLE session_question (
    session_id      UUID         NOT NULL REFERENCES study_session (id) ON DELETE CASCADE,
    position        INTEGER      NOT NULL,
    question_id     VARCHAR(120) NOT NULL REFERENCES question (id),
    PRIMARY KEY (session_id, position),
    UNIQUE (session_id, question_id)
);

CREATE TABLE answer_attempt (
    id                  UUID         PRIMARY KEY,
    session_id          UUID         NOT NULL REFERENCES study_session (id),
    user_id             UUID         NOT NULL REFERENCES app_user (id),
    question_id         VARCHAR(120) NOT NULL REFERENCES question (id),
    question_revision   INTEGER      NOT NULL,
    topic_id            VARCHAR(100) NOT NULL REFERENCES topic (id),
    selected_option     VARCHAR(1)    NOT NULL,
    correct             BOOLEAN      NOT NULL,
    answered_at         TIMESTAMPTZ  NOT NULL,
    UNIQUE (session_id, question_id)
);
CREATE INDEX idx_attempt_user_topic ON answer_attempt (user_id, topic_id, answered_at DESC);
CREATE INDEX idx_attempt_user_question ON answer_attempt (user_id, question_id, answered_at DESC);

-- ---------- Progresso, revisão e XP ------------------------------------

CREATE TABLE user_topic_progress (
    user_id             UUID         NOT NULL REFERENCES app_user (id),
    topic_id            VARCHAR(100) NOT NULL REFERENCES topic (id),
    state               VARCHAR(30)  NOT NULL,
    lessons_completed   INTEGER      NOT NULL DEFAULT 0,
    attempts            INTEGER      NOT NULL DEFAULT 0,
    correct             INTEGER      NOT NULL DEFAULT 0,
    -- últimos resultados, do mais antigo ao mais recente ('1' acerto, '0' erro)
    recent_results      VARCHAR(20)  NOT NULL DEFAULT '',
    last_studied_at     TIMESTAMPTZ,
    mastery_reached_at  TIMESTAMPTZ,
    PRIMARY KEY (user_id, topic_id)
);

CREATE TABLE review_schedule (
    id                  UUID         PRIMARY KEY,
    user_id             UUID         NOT NULL REFERENCES app_user (id),
    topic_id            VARCHAR(100) NOT NULL REFERENCES topic (id),
    step                INTEGER      NOT NULL,
    interval_days       INTEGER      NOT NULL,
    due_at              TIMESTAMPTZ  NOT NULL,
    successful_reviews  INTEGER      NOT NULL DEFAULT 0,
    policy              VARCHAR(50)  NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    UNIQUE (user_id, topic_id)
);
CREATE INDEX idx_review_user_due ON review_schedule (user_id, due_at);

-- Histórico imutável das revisões: permite trocar a política sem perder evidências.
CREATE TABLE review_log (
    id                  UUID         PRIMARY KEY,
    review_id           UUID         NOT NULL REFERENCES review_schedule (id),
    session_id          UUID         REFERENCES study_session (id),
    reviewed_at         TIMESTAMPTZ  NOT NULL,
    score               NUMERIC(4,3) NOT NULL,
    previous_step       INTEGER      NOT NULL,
    new_step            INTEGER      NOT NULL,
    new_interval_days   INTEGER      NOT NULL,
    policy              VARCHAR(50)  NOT NULL
);

CREATE TABLE xp_event (
    id              UUID         PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES app_user (id),
    amount          INTEGER      NOT NULL,
    reason          VARCHAR(50)  NOT NULL,
    reference_id    VARCHAR(140),
    created_at      TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_xp_user ON xp_event (user_id);
