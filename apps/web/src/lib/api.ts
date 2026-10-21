import type {
  AnswerResponse,
  Exam,
  Journey,
  Me,
  Mission,
  PendingReview,
  Preferences,
  ProgressSummary,
  Role,
  Session,
  StartSessionRequest,
  Syllabus,
} from "./types";

/** Erro da API no formato Problem Details (RFC 9457). */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
    readonly fields?: Record<string, string>,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let res: Response;
  try {
    res = await fetch(path, {
      ...init,
      credentials: "same-origin",
      headers: { "Content-Type": "application/json", ...(init?.headers ?? {}) },
    });
  } catch {
    throw new ApiError(0, "Não foi possível conectar. Verifique sua internet e tente novamente.");
  }
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  const body = text ? JSON.parse(text) : undefined;
  if (!res.ok) {
    const message =
      body?.detail ?? (res.status === 401 ? "Sua sessão expirou. Entre novamente." : "Algo deu errado. Tente novamente.");
    throw new ApiError(res.status, message, body?.fields);
  }
  return body as T;
}

const json = (body: unknown) => JSON.stringify(body);

export const api = {
  register: (email: string, password: string, displayName: string) =>
    request<Me>("/api/v1/auth/register", { method: "POST", body: json({ email, password, displayName }) }),
  login: (email: string, password: string) =>
    request<Me>("/api/v1/auth/login", { method: "POST", body: json({ email, password }) }),
  logout: () => request<void>("/api/v1/auth/logout", { method: "POST" }),
  me: () => request<Me>("/api/v1/me"),
  updateMe: (patch: Partial<Preferences> & { selectedRoleId?: string; displayName?: string }) =>
    request<Me>("/api/v1/me", { method: "PATCH", body: json(patch) }),

  exams: () => request<Exam[]>("/api/v1/exams"),
  roles: (examId: string) => request<Role[]>(`/api/v1/exams/${encodeURIComponent(examId)}/roles`),
  syllabus: (roleId: string) => request<Syllabus>(`/api/v1/roles/${encodeURIComponent(roleId)}/syllabus`),

  journey: () => request<Journey>("/api/v1/me/journey"),
  progress: () => request<ProgressSummary>("/api/v1/me/progress"),
  reviews: () => request<PendingReview[]>("/api/v1/me/reviews"),

  startSession: (req: StartSessionRequest) =>
    request<Session>("/api/v1/study-sessions", { method: "POST", body: json(req) }),
  session: (id: string) => request<Session>(`/api/v1/study-sessions/${id}`),
  answer: (id: string, questionId: string, selectedOption: string) =>
    request<AnswerResponse>(`/api/v1/study-sessions/${id}/answers`, {
      method: "POST",
      body: json({ questionId, selectedOption }),
    }),
  updateSession: (id: string, body: { action?: "PAUSE" | "RESUME" | "COMPLETE"; currentStep?: number }) =>
    request<Session>(`/api/v1/study-sessions/${id}`, { method: "PATCH", body: json(body) }),
  completeReview: (reviewId: string, sessionId: string) =>
    request<Session>(`/api/v1/me/reviews/${reviewId}/complete`, { method: "POST", body: json({ sessionId }) }),
};

export function missionToRequest(m: Mission): StartSessionRequest {
  return { kind: m.kind, missionType: m.missionType, lessonId: m.lessonId, topicId: m.topicId, reviewId: m.reviewId };
}
