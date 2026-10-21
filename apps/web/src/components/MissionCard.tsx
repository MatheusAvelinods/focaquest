import { missionTypeLabel, sessionKindLabel } from "@/lib/labels";
import type { Mission } from "@/lib/types";

import { Button } from "./ui";

export function missionTitle(m: Mission): string {
  if (m.kind === "REVIEW") return `Revisar: ${m.topicName}`;
  if (m.kind === "REINFORCEMENT") return `Reforçar: ${m.topicName}`;
  return m.lessonTitle ?? m.topicName;
}

/** Card da missão recomendada: o que fazer, quanto tempo leva e por quê. */
export function MissionCard({ mission, onStart, busy }: { mission: Mission; onStart: () => void; busy?: boolean }) {
  return (
    <section aria-labelledby="missao-recomendada" className="rounded-3xl border border-brand/30 bg-brand-soft p-6 sm:p-8">
      <p className="text-sm font-semibold uppercase tracking-wide text-brand-strong">Missão recomendada</p>
      <h2 id="missao-recomendada" className="mt-2 text-2xl font-bold sm:text-3xl">
        {missionTitle(mission)}
      </h2>
      <p className="mt-1 text-muted">
        {mission.subjectName} · {sessionKindLabel[mission.kind]} · {missionTypeLabel[mission.missionType]}, cerca de{" "}
        {mission.estimatedMinutes} min
      </p>
      <p className="mt-4 rounded-xl bg-surface/70 px-4 py-3 text-sm">
        <span className="font-semibold">Por que esta missão? </span>
        {mission.reason}
      </p>
      <Button onClick={onStart} disabled={busy} className="mt-6 w-full text-lg sm:w-auto">
        {busy ? "Preparando…" : "Começar missão"}
      </Button>
    </section>
  );
}
