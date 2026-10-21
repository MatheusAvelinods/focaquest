package com.focaquest.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.focaquest.support.ApiClient;
import com.focaquest.support.IntegrationTest;

class SecurityIT extends IntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void rotasDoEstudanteExigemAutenticacao() {
        var anon = new ApiClient(mvc);
        assertThat(anon.get("/api/v1/me").status()).isEqualTo(401);
        assertThat(anon.get("/api/v1/me/journey").status()).isEqualTo(401);
        assertThat(anon.get("/api/v1/exams").status()).isEqualTo(200);
    }

    @Test
    void estudanteNaoAcessaSessaoDeOutroEstudante() {
        var alice = new ApiClient(mvc).register();
        var bob = new ApiClient(mvc).register();
        alice.patch("/api/v1/me", "{\"selectedRoleId\":\"tjpb-2026-analista-ti-dev\"}");
        String sessionId = alice.post("/api/v1/study-sessions",
                "{\"kind\":\"LESSON\",\"lessonId\":\"l.pt.crase\"}").body().get("id").asString();

        assertThat(alice.get("/api/v1/study-sessions/" + sessionId).status()).isEqualTo(200);
        assertThat(bob.get("/api/v1/study-sessions/" + sessionId).status()).isEqualTo(404);
        assertThat(bob.post("/api/v1/study-sessions/" + sessionId + "/answers",
                "{\"questionId\":\"q.pt.crase.01\",\"selectedOption\":\"C\"}").status()).isEqualTo(404);
    }

    @Test
    void cadastroValidaDadosEImpedeEmailDuplicado() {
        var anon = new ApiClient(mvc);
        assertThat(anon.post("/api/v1/auth/register", "{\"email\":\"invalido\",\"password\":\"123\",\"displayName\":\"\"}")
                .status()).isEqualTo(400);
        String body = "{\"email\":\"dup@exemplo.com\",\"password\":\"senha-segura-123\",\"displayName\":\"Dup\"}";
        anon.post("/api/v1/auth/register", body);
        assertThat(anon.post("/api/v1/auth/register", body).status()).isEqualTo(409);
        assertThat(anon.post("/api/v1/auth/login", "{\"email\":\"dup@exemplo.com\",\"password\":\"errada-123\"}")
                .status()).isEqualTo(401);
        assertThat(anon.post("/api/v1/auth/login", "{\"email\":\"dup@exemplo.com\",\"password\":\"senha-segura-123\"}")
                .status()).isEqualTo(200);
    }
}
