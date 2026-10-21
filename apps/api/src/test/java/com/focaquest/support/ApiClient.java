package com.focaquest.support;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Cliente HTTP mínimo para os testes de ponta a ponta da API, autenticado por cookie. */
public class ApiClient {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final MockMvc mvc;
    private Cookie session;

    public ApiClient(MockMvc mvc) {
        this.mvc = mvc;
    }

    public record Response(int status, JsonNode body, MockHttpServletResponse raw) {
    }

    public ApiClient register() {
        String email = "estudante-" + UUID.randomUUID() + "@exemplo.com";
        Response r = post("/api/v1/auth/register",
                "{\"email\":\"" + email + "\",\"password\":\"senha-segura-123\",\"displayName\":\"Estudante\"}");
        if (r.status() != 201) {
            throw new AssertionError("Cadastro falhou: " + r.status() + " " + r.body());
        }
        this.session = r.raw().getCookie("fq_session");
        return this;
    }

    public Response get(String url) {
        return exec(MockMvcRequestBuilders.get(url));
    }

    public Response post(String url, String json) {
        return exec(MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    public Response patch(String url, String json) {
        return exec(MockMvcRequestBuilders.patch(url)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private Response exec(MockHttpServletRequestBuilder req) {
        try {
            if (session != null) {
                req.cookie(session);
            }
            MockHttpServletResponse res = mvc.perform(req).andReturn().getResponse();
            String body = res.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            return new Response(res.getStatus(), body.isBlank() ? JSON.nullNode() : JSON.readTree(body), res);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
