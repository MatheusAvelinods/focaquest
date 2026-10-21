package com.focaquest.learning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.focaquest.questions.QuestionRepository;
import com.focaquest.support.ApiClient;
import com.focaquest.support.IntegrationTest;
import com.focaquest.support.MutableClock;

import tools.jackson.databind.JsonNode;

/**
 * Critério de sucesso do MVP (PRD, seção 10), de ponta a ponta pela API: selecionar o cargo,
 * iniciar uma missão, aprender, responder, receber feedback, interromper e retomar, consultar
 * o progresso e executar a revisão recomendada.
 */
class MvpFlowIT extends IntegrationTest {

    static final String ROLE = "tjpb-2026-analista-ti-dev";

    @Autowired
    MockMvc mvc;
    @Autowired
    MutableClock clock;
    @Autowired
    QuestionRepository questions;

    ApiClient api;

    @BeforeEach
    void setUp() {
        api = new ApiClient(mvc).register();
    }

    @Test
    void cicloCompletoDeAprendizagem() throws Exception {
        // 1. Selecionar TJPB e Desenvolvimento de Sistemas
        assertThat(api.get("/api/v1/me/journey").body().get("needsOnboarding").asBoolean()).isTrue();
        var exams = api.get("/api/v1/exams").body();
        assertThat(exams.get(0).get("id").asString()).isEqualTo("tjpb-2026");
        assertThat(api.get("/api/v1/exams/tjpb-2026/roles").body().get(0).get("id").asString()).isEqualTo(ROLE);
        assertThat(api.patch("/api/v1/me", "{\"selectedRoleId\":\"" + ROLE + "\"}").status()).isEqualTo(200);

        // 2. A tela inicial já responde "o que estudar agora?"
        JsonNode journey = api.get("/api/v1/me/journey").body();
        assertThat(journey.get("needsOnboarding").asBoolean()).isFalse();
        JsonNode mission = journey.get("recommended");
        assertThat(mission.get("kind").asString()).isEqualTo("LESSON");
        assertThat(mission.get("reason").asString()).isNotBlank();
        assertThat(journey.get("shortAlternative").get("missionType").asString()).isEqualTo("MICRO");
        String lessonId = mission.get("lessonId").asString();

        // 3. Iniciar a missão e aprender o conceito
        var started = api.post("/api/v1/study-sessions",
                "{\"kind\":\"LESSON\",\"lessonId\":\"" + lessonId + "\",\"missionType\":\"STANDARD\"}");
        assertThat(started.status()).isEqualTo(201);
        JsonNode session = started.body();
        String sessionId = session.get("id").asString();
        assertThat(session.get("lesson").get("explanation").asString()).isNotBlank();
        assertThat(session.get("lesson").get("example").asString()).isNotBlank();
        assertThat(stepTypes(session)).startsWith("OBJECTIVE", "EXPLANATION", "EXAMPLE").endsWith("SUMMARY");

        // Integridade: gabarito e explicação não são enviados antes da resposta.
        String raw = started.raw().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(raw).doesNotContain("correctOption").doesNotContain("rationale");
        List<String> questionIds = questionIds(session);
        assertThat(questionIds).hasSizeGreaterThanOrEqualTo(2);

        // 4 e 5. Resolver uma questão e receber feedback comentado
        String first = questionIds.getFirst();
        String correct = questions.findById(first).orElseThrow().getCorrectOption();
        JsonNode answer = api.post("/api/v1/study-sessions/" + sessionId + "/answers",
                "{\"questionId\":\"" + first + "\",\"selectedOption\":\"" + correct + "\"}").body();
        assertThat(answer.get("feedback").get("correct").asBoolean()).isTrue();
        assertThat(answer.get("feedback").get("explanation").asString()).isNotBlank();
        assertThat(answer.get("feedback").get("options")).hasSize(5);
        assertThat(answer.get("feedback").get("options").get(0).get("rationale").asString()).isNotBlank();
        assertThat(answer.get("xpEarned").asInt()).isPositive();

        // Responder de novo não altera a tentativa original.
        String wrong = correct.equals("A") ? "B" : "A";
        JsonNode again = api.post("/api/v1/study-sessions/" + sessionId + "/answers",
                "{\"questionId\":\"" + first + "\",\"selectedOption\":\"" + wrong + "\"}").body();
        assertThat(again.get("alreadyAnswered").asBoolean()).isTrue();
        assertThat(again.get("feedback").get("selectedOption").asString()).isEqualTo(correct);

        // 6. Interromper e retomar sem refazer o que já foi respondido
        assertThat(api.patch("/api/v1/study-sessions/" + sessionId, "{\"action\":\"PAUSE\"}")
                .body().get("status").asString()).isEqualTo("PAUSED");
        JsonNode interrupted = api.get("/api/v1/me/journey").body().get("interrupted");
        assertThat(interrupted.get("sessionId").asString()).isEqualTo(sessionId);
        assertThat(interrupted.get("answered").asInt()).isEqualTo(1);

        JsonNode resumed = api.patch("/api/v1/study-sessions/" + sessionId, "{\"action\":\"RESUME\"}").body();
        assertThat(resumed.get("status").asString()).isEqualTo("IN_PROGRESS");
        assertThat(resumed.get("answeredCount").asInt()).isEqualTo(1);
        JsonNode firstStep = questionStep(resumed, first);
        assertThat(firstStep.get("feedback").get("correct").asBoolean()).isTrue();

        // Concluir antes de responder tudo é recusado.
        assertThat(api.patch("/api/v1/study-sessions/" + sessionId, "{\"action\":\"COMPLETE\"}").status())
                .isEqualTo(422);

        for (String q : questionIds.subList(1, questionIds.size())) {
            String opt = questions.findById(q).orElseThrow().getCorrectOption();
            api.post("/api/v1/study-sessions/" + sessionId + "/answers",
                    "{\"questionId\":\"" + q + "\",\"selectedOption\":\"" + opt + "\"}");
        }
        JsonNode done = api.patch("/api/v1/study-sessions/" + sessionId, "{\"action\":\"COMPLETE\"}").body();
        assertThat(done.get("status").asString()).isEqualTo("COMPLETED");
        assertThat(done.get("result").get("correct").asInt()).isEqualTo(questionIds.size());
        assertThat(done.get("result").get("xpAwarded").asInt()).isPositive();
        assertThat(done.get("result").get("nextReviewAt").isNull()).isFalse();
        String topicId = done.get("topic").get("id").asString();

        // 7. Consultar o progresso
        JsonNode progress = api.get("/api/v1/me/progress").body();
        assertThat(progress.get("edital").get("topicsStarted").asInt()).isEqualTo(1);
        assertThat(progress.get("edital").get("coveragePercent").asDouble()).isPositive();
        assertThat(progress.get("questions").get("accuracy").asDouble()).isEqualTo(1.0);
        assertThat(progress.get("xp").asLong()).isPositive();
        assertThat(topicState(progress, topicId)).isEqualTo("IN_REVIEW");

        // Revisão ainda não venceu: a próxima recomendação é conteúdo novo.
        assertThat(api.get("/api/v1/me/journey").body().get("recommended").get("kind").asString()).isEqualTo("LESSON");

        // 8. Um dia depois, a revisão recomendada aparece e é executada
        clock.advance(Duration.ofDays(1).plusMinutes(1));
        JsonNode rec = api.get("/api/v1/me/journey").body().get("recommended");
        assertThat(rec.get("kind").asString()).isEqualTo("REVIEW");
        assertThat(rec.get("topicId").asString()).isEqualTo(topicId);
        String reviewId = rec.get("reviewId").asString();

        JsonNode review = api.post("/api/v1/study-sessions",
                "{\"kind\":\"REVIEW\",\"reviewId\":\"" + reviewId + "\",\"missionType\":\"STANDARD\"}").body();
        assertThat(stepTypes(review)).doesNotContain("EXPLANATION");
        String reviewSession = review.get("id").asString();
        for (String q : questionIds(review)) {
            String opt = questions.findById(q).orElseThrow().getCorrectOption();
            api.post("/api/v1/study-sessions/" + reviewSession + "/answers",
                    "{\"questionId\":\"" + q + "\",\"selectedOption\":\"" + opt + "\"}");
        }
        JsonNode reviewed = api.post("/api/v1/me/reviews/" + reviewId + "/complete",
                "{\"sessionId\":\"" + reviewSession + "\"}").body();
        assertThat(reviewed.get("status").asString()).isEqualTo("COMPLETED");

        // Acerto total sobe um degrau: próxima revisão em 3 dias.
        JsonNode reviews = api.get("/api/v1/me/reviews").body();
        assertThat(reviews).hasSize(1);
        assertThat(reviews.get(0).get("due").asBoolean()).isFalse();
        var nextDue = java.time.Instant.parse(reviews.get(0).get("dueAt").asString());
        assertThat(Duration.between(clock.instant(), nextDue)).isEqualTo(Duration.ofDays(3));
    }

    @Test
    void errosLevamAReforcoDoAssunto() {
        api.patch("/api/v1/me", "{\"selectedRoleId\":\"" + ROLE + "\"}");
        String lessonId = api.get("/api/v1/me/journey").body().get("recommended").get("lessonId").asString();
        JsonNode session = api.post("/api/v1/study-sessions",
                "{\"kind\":\"LESSON\",\"lessonId\":\"" + lessonId + "\",\"missionType\":\"STANDARD\"}").body();
        String sessionId = session.get("id").asString();
        for (String q : questionIds(session)) {
            String correct = questions.findById(q).orElseThrow().getCorrectOption();
            String wrong = correct.equals("A") ? "B" : "A";
            api.post("/api/v1/study-sessions/" + sessionId + "/answers",
                    "{\"questionId\":\"" + q + "\",\"selectedOption\":\"" + wrong + "\"}");
        }
        JsonNode done = api.patch("/api/v1/study-sessions/" + sessionId, "{\"action\":\"COMPLETE\"}").body();
        assertThat(done.get("result").get("topicState").asString()).isEqualTo("NEEDS_REINFORCEMENT");

        JsonNode rec = api.get("/api/v1/me/journey").body().get("recommended");
        assertThat(rec.get("kind").asString()).isEqualTo("REINFORCEMENT");
        assertThat(rec.get("topicId").asString()).isEqualTo(done.get("topic").get("id").asString());
    }

    @Test
    void preferenciaDeMissaoCurtaDefineAMissaoRecomendada() {
        api.patch("/api/v1/me", "{\"selectedRoleId\":\"" + ROLE + "\",\"preferredMissionType\":\"MICRO\"}");
        JsonNode rec = api.get("/api/v1/me/journey").body().get("recommended");
        assertThat(rec.get("missionType").asString()).isEqualTo("MICRO");
        JsonNode session = api.post("/api/v1/study-sessions",
                "{\"kind\":\"LESSON\",\"lessonId\":\"" + rec.get("lessonId").asString() + "\",\"missionType\":\"MICRO\"}")
                .body();
        assertThat(questionIds(session)).hasSize(2);
    }

    @Test
    void iniciarAMesmaMissaoDuasVezesRetomaASessaoAberta() {
        api.patch("/api/v1/me", "{\"selectedRoleId\":\"" + ROLE + "\"}");
        String body = "{\"kind\":\"LESSON\",\"lessonId\":\"l.pt.crase\",\"missionType\":\"STANDARD\"}";
        String a = api.post("/api/v1/study-sessions", body).body().get("id").asString();
        String b = api.post("/api/v1/study-sessions", body).body().get("id").asString();
        assertThat(b).isEqualTo(a);
    }

    private static List<String> stepTypes(JsonNode session) {
        List<String> out = new ArrayList<>();
        session.get("steps").forEach(s -> out.add(s.get("type").asString()));
        return out;
    }

    private static List<String> questionIds(JsonNode session) {
        List<String> out = new ArrayList<>();
        session.get("steps").forEach(s -> {
            if (s.get("type").asString().equals("QUESTION")) out.add(s.get("question").get("id").asString());
        });
        return out;
    }

    private static JsonNode questionStep(JsonNode session, String questionId) {
        for (JsonNode s : session.get("steps")) {
            if (s.get("type").asString().equals("QUESTION") && s.get("question").get("id").asString().equals(questionId)) {
                return s;
            }
        }
        throw new AssertionError("Questão não encontrada na sessão: " + questionId);
    }

    private static String topicState(JsonNode progress, String topicId) {
        for (JsonNode t : progress.get("topics")) {
            if (t.get("topicId").asString().equals(topicId)) return t.get("state").asString();
        }
        throw new AssertionError("Tópico ausente do progresso: " + topicId);
    }
}
