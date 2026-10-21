"use client";

import { useCallback, useEffect, useState } from "react";

import { AppShell } from "@/components/AppShell";
import { Button, Card, ErrorMessage, Loading } from "@/components/ui";
import { api, ApiError } from "@/lib/api";
import { relativeDay } from "@/lib/labels";
import type { PendingReview } from "@/lib/types";
import { UserProvider, useUser } from "@/lib/user";
import { useStartMission } from "@/lib/useStartMission";

function Reviews() {
  const { user } = useUser();
  const [reviews, setReviews] = useState<PendingReview[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const { start, busy, error: startError } = useStartMission();

  const load = useCallback(async () => {
    try {
      setReviews(await api.reviews());
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Não foi possível carregar as revisões.");
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  if (error) return <ErrorMessage message={error} onRetry={load} />;
  if (!reviews) return <Loading label="Carregando revisões" />;

  const due = reviews.filter((r) => r.due);
  const upcoming = reviews.filter((r) => !r.due);
  const missionType = user?.preferences.preferredMissionType ?? "STANDARD";

  const row = (r: PendingReview) => (
    <li key={r.reviewId} className="flex flex-col gap-2 py-3 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <p className="font-medium">{r.topicName}</p>
        <p className="text-sm text-muted">{r.due ? "Pendente" : `Programada para ${relativeDay(r.dueAt)}`}</p>
      </div>
      <Button
        variant={r.due ? "primary" : "secondary"}
        disabled={busy}
        onClick={() => start({ kind: "REVIEW", reviewId: r.reviewId, missionType })}
      >
        {r.due ? "Revisar agora" : "Antecipar"}
      </Button>
    </li>
  );

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-bold sm:text-3xl">Revisões</h1>
        <p className="text-muted">
          Os intervalos crescem quando você acerta (1, 3, 7, 14, 30 e 60 dias) e encurtam quando é preciso reforçar.
        </p>
      </header>
      {startError && <ErrorMessage message={startError} />}
      <Card>
        <h2 className="font-semibold">Para hoje</h2>
        {due.length === 0 ? (
          <p className="mt-2 text-sm text-muted">Nenhuma revisão pendente. Bom momento para um conteúdo novo!</p>
        ) : (
          <ul className="divide-y divide-line">{due.map(row)}</ul>
        )}
      </Card>
      {upcoming.length > 0 && (
        <Card>
          <h2 className="font-semibold">Próximas</h2>
          <ul className="divide-y divide-line">{upcoming.map(row)}</ul>
        </Card>
      )}
    </div>
  );
}

export default function RevisoesPage() {
  return (
    <UserProvider>
      <AppShell>
        <Reviews />
      </AppShell>
    </UserProvider>
  );
}
