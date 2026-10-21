"use client";

import { useCallback, useEffect, useState } from "react";

import { AppShell } from "@/components/AppShell";
import { Card, ErrorMessage, Loading, ProgressBar } from "@/components/ui";
import { api, ApiError } from "@/lib/api";
import { percent } from "@/lib/labels";
import type { ProgressSummary } from "@/lib/types";
import { UserProvider } from "@/lib/user";

function Progress() {
  const [data, setData] = useState<ProgressSummary | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setData(await api.progress());
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Não foi possível carregar o progresso.");
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  if (error) return <ErrorMessage message={error} onRetry={load} />;
  if (!data) return <Loading label="Carregando seu progresso" />;

  const mastered = data.topics.filter((t) => t.masteryReachedAt);
  const reinforce = data.topics.filter((t) => t.state === "NEEDS_REINFORCEMENT");

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-bold sm:text-3xl">Seu progresso</h1>
        <p className="text-muted">Ausências não apagam nada: tudo o que você aprendeu continua aqui.</p>
      </header>

      <div className="grid gap-4 sm:grid-cols-3">
        <Card>
          <h2 className="text-sm font-semibold text-muted">Edital percorrido</h2>
          <p className="mt-1 text-3xl font-bold">{Math.round(data.edital.coveragePercent)}%</p>
          <ProgressBar value={data.edital.coveragePercent} label="Edital percorrido" className="mt-3" />
          <p className="mt-2 text-sm text-muted">
            {data.edital.topicsStarted} de {data.edital.topicsTotal} assuntos
          </p>
        </Card>
        <Card>
          <h2 className="text-sm font-semibold text-muted">Desempenho nas questões</h2>
          <p className="mt-1 text-3xl font-bold">{data.questions.attempts ? percent(data.questions.accuracy) : "—"}</p>
          <p className="mt-2 text-sm text-muted">
            {data.questions.correct} acertos em {data.questions.attempts} respostas
          </p>
        </Card>
        <Card>
          <h2 className="text-sm font-semibold text-muted">Domínio e engajamento</h2>
          <p className="mt-1 text-3xl font-bold">{data.edital.topicsMastered}</p>
          <p className="text-sm text-muted">assuntos com domínio estimado</p>
          <p className="mt-2 text-sm">
            <strong>{data.xp} XP</strong> <span className="text-muted">— mede o quanto você praticou, não o quanto domina.</span>
          </p>
        </Card>
      </div>

      {reinforce.length > 0 && (
        <Card>
          <h2 className="font-semibold">O que ainda preciso reforçar?</h2>
          <ul className="mt-2 list-disc space-y-1 pl-6">
            {reinforce.map((t) => (
              <li key={t.topicId}>
                {t.name} <span className="text-muted">({percent(t.accuracy)} de acerto)</span>
              </li>
            ))}
          </ul>
        </Card>
      )}

      <Card>
        <h2 className="font-semibold">Por disciplina</h2>
        <div className="mt-3 overflow-x-auto">
          <table className="w-full min-w-[32rem] text-left text-sm">
            <caption className="sr-only">Progresso e desempenho por disciplina</caption>
            <thead className="text-muted">
              <tr>
                <th scope="col" className="py-2 pr-3 font-medium">Disciplina</th>
                <th scope="col" className="py-2 pr-3 font-medium">Assuntos iniciados</th>
                <th scope="col" className="py-2 pr-3 font-medium">Domínio</th>
                <th scope="col" className="py-2 font-medium">Acertos</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-line">
              {data.subjects.map((s) => (
                <tr key={s.subjectId}>
                  <th scope="row" className="py-2 pr-3 font-medium">{s.name}</th>
                  <td className="py-2 pr-3">
                    {s.topicsStarted}/{s.topicsTotal}
                  </td>
                  <td className="py-2 pr-3">{s.topicsMastered}</td>
                  <td className="py-2">{s.attempts ? `${percent(s.accuracy)} (${s.attempts})` : "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      <Card>
        <h2 className="font-semibold">Marcos de domínio</h2>
        {mastered.length === 0 ? (
          <p className="mt-2 text-sm text-muted">
            Um assunto atinge o domínio estimado depois de bons resultados em revisões espaçadas. Seu primeiro marco está a caminho.
          </p>
        ) : (
          <ul className="mt-2 space-y-1">
            {mastered.map((t) => (
              <li key={t.topicId}>🏅 {t.name}</li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  );
}

export default function ProgressoPage() {
  return (
    <UserProvider>
      <AppShell>
        <Progress />
      </AppShell>
    </UserProvider>
  );
}
