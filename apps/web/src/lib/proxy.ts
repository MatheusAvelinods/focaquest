/**
 * Encaminha /api/* para a API Spring Boot. O endereço da API é lido em tempo de execução
 * (FOCAQUEST_API_URL), então a mesma imagem funciona em qualquer ambiente. O navegador só fala
 * com a origem do frontend: o cookie de sessão (httpOnly, SameSite=Strict) dispensa CORS.
 */

// Cabeçalhos que não devem atravessar o proxy (hop-by-hop ou recalculados pelo fetch).
const DROP_REQUEST = new Set(["host", "connection", "content-length", "accept-encoding", "transfer-encoding"]);
const DROP_RESPONSE = new Set(["connection", "content-length", "content-encoding", "transfer-encoding", "keep-alive"]);

export function apiBaseUrl(): string {
  return (process.env.FOCAQUEST_API_URL ?? "http://localhost:8080").replace(/\/+$/, "");
}

export function forwardRequestHeaders(source: Headers): Headers {
  const out = new Headers();
  source.forEach((value, key) => {
    if (!DROP_REQUEST.has(key.toLowerCase())) out.set(key, value);
  });
  return out;
}

export function forwardResponseHeaders(source: Headers): Headers {
  const out = new Headers();
  source.forEach((value, key) => {
    const k = key.toLowerCase();
    if (!DROP_RESPONSE.has(k) && k !== "set-cookie") out.set(key, value);
  });
  // Vários cookies precisam ser repassados um a um.
  for (const cookie of source.getSetCookie()) out.append("set-cookie", cookie);
  return out;
}

export async function proxy(request: Request, path: string[]): Promise<Response> {
  const incoming = new URL(request.url);
  const target = `${apiBaseUrl()}/api/${path.map(encodeURIComponent).join("/")}${incoming.search}`;
  const hasBody = request.method !== "GET" && request.method !== "HEAD";
  try {
    const res = await fetch(target, {
      method: request.method,
      headers: forwardRequestHeaders(request.headers),
      body: hasBody ? await request.arrayBuffer() : undefined,
      redirect: "manual",
      cache: "no-store",
    });
    return new Response(res.status === 204 ? null : await res.arrayBuffer(), {
      status: res.status,
      headers: forwardResponseHeaders(res.headers),
    });
  } catch {
    return Response.json(
      { title: "API indisponível", detail: "O servidor está iniciando ou indisponível. Tente novamente em instantes." },
      { status: 503 },
    );
  }
}
