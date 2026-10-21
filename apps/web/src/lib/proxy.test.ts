import { afterEach, describe, expect, it, vi } from "vitest";

import { apiBaseUrl, forwardRequestHeaders, forwardResponseHeaders, proxy } from "./proxy";

describe("proxy da API", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.unstubAllEnvs();
  });

  it("lê o endereço da API em tempo de execução", () => {
    vi.stubEnv("FOCAQUEST_API_URL", "https://api.exemplo.com/");
    expect(apiBaseUrl()).toBe("https://api.exemplo.com");
  });

  it("repassa cookies e descarta cabeçalhos hop-by-hop", () => {
    const req = forwardRequestHeaders(new Headers({ cookie: "fq_session=abc", host: "web", connection: "keep-alive" }));
    expect(req.get("cookie")).toBe("fq_session=abc");
    expect(req.has("host")).toBe(false);

    const source = new Headers({ "content-type": "application/json", "content-encoding": "gzip" });
    source.append("set-cookie", "a=1; HttpOnly");
    source.append("set-cookie", "b=2; HttpOnly");
    const res = forwardResponseHeaders(source);
    expect(res.getSetCookie()).toEqual(["a=1; HttpOnly", "b=2; HttpOnly"]);
    expect(res.has("content-encoding")).toBe(false);
  });

  it("encaminha método, caminho, query e corpo", async () => {
    vi.stubEnv("FOCAQUEST_API_URL", "http://api:8080");
    const fetchMock = vi.fn().mockResolvedValue(new Response('{"ok":true}', { status: 201 }));
    vi.stubGlobal("fetch", fetchMock);

    const res = await proxy(
      new Request("http://web/api/v1/study-sessions?x=1", { method: "POST", body: '{"kind":"LESSON"}' }),
      ["v1", "study-sessions"],
    );
    expect(res.status).toBe(201);
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://api:8080/api/v1/study-sessions?x=1");
    expect(init.method).toBe("POST");
    expect(new TextDecoder().decode(init.body)).toBe('{"kind":"LESSON"}');
  });

  it("responde 503 amigável quando a API está fora do ar", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new Error("ECONNREFUSED")));
    const res = await proxy(new Request("http://web/api/v1/me"), ["v1", "me"]);
    expect(res.status).toBe(503);
    expect((await res.json()).detail).toMatch(/iniciando/);
  });
});
