import type { MissionType, SessionKind, StudyStrategy, TopicState } from "./types";

export const topicStateLabel: Record<TopicState, string> = {
  NOT_STARTED: "Não iniciado",
  LEARNING: "Em aprendizagem",
  NEEDS_REINFORCEMENT: "Precisa de reforço",
  IN_REVIEW: "Em revisão",
  MASTERED: "Domínio estimado",
};

export const missionTypeLabel: Record<MissionType, string> = {
  MICRO: "Micro",
  STANDARD: "Padrão",
  DEEP: "Aprofundada",
};

export const missionTypeHint: Record<MissionType, string> = {
  MICRO: "cerca de 3–5 min",
  STANDARD: "cerca de 8–12 min",
  DEEP: "cerca de 15–25 min",
};

export const sessionKindLabel: Record<SessionKind, string> = {
  LESSON: "Nova lição",
  REINFORCEMENT: "Reforço",
  REVIEW: "Revisão",
};

export const strategyLabel: Record<StudyStrategy, { title: string; hint: string }> = {
  EDITAL_ORDER: { title: "Seguir a ordem do edital", hint: "Avança assunto por assunto, como no edital." },
  UNSTUDIED_FIRST: { title: "Priorizar o que ainda não estudei", hint: "Cobre o edital mais rápido." },
  LOW_PERFORMANCE: { title: "Priorizar onde estou errando", hint: "Reforça assuntos com baixo desempenho." },
  BY_SUBJECT: { title: "Estudar por disciplina", hint: "Concentra as missões em uma disciplina." },
  REVIEW_FIRST: { title: "Revisar conteúdos anteriores", hint: "Dá prioridade às revisões, inclusive antecipadas." },
  BOARD_FOCUS: { title: "Treinar no estilo da banca", hint: "Prioriza assuntos com mais questões no formato da banca." },
};

/** Formata uma proporção (0–1) como porcentagem inteira. */
export function percent(ratio: number): string {
  return `${Math.round(ratio * 100)}%`;
}

/** Data relativa amigável para revisões: "hoje", "amanhã", "em 3 dias", "há 2 dias". */
export function relativeDay(iso: string, now: Date = new Date()): string {
  const day = (d: Date) => Date.UTC(d.getFullYear(), d.getMonth(), d.getDate());
  const diff = Math.round((day(new Date(iso)) - day(now)) / 86_400_000);
  if (diff === 0) return "hoje";
  if (diff === 1) return "amanhã";
  if (diff === -1) return "ontem";
  return diff > 0 ? `em ${diff} dias` : `há ${-diff} dias`;
}
