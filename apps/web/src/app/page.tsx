"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";

import { AppShell } from "@/components/AppShell";
import { MissionCard, missionTitle } from "@/components/MissionCard";
import { Button, ButtonLink, Card, ErrorMessage, Loading, ProgressBar } from "@/components/ui";
import { api, ApiError } from "@/lib/api";
import { percent, relativeDay, sessionKindLabel } from "@/lib/labels";
import type { Journey } from "@/lib/types";
import { UserProvider, useUser } from "@/lib/user";
import { useStartMission } from "@/lib/useStartMission";

function JourneyView() {
  const router = useRouter();
  const { user } = useUser();
  const [journey, setJourney] = useState<Journey | null>(null);
  const [error, setError] = useState<string | null>(null);
  const { start, busy, error: startError } = useStartMission();
  const focus = user?.preferences.focusMode ?? false;

  const load = useCallback(async () => {
    setError(null);
    try {
      const j = await api.journey();
      if (j.needsOnboarding) {
        router.replace("/onboarding");
        return;
      }
      setJourney(j);
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) return;
      setError(e instanceof ApiError ? e.message : "Não foi possível carregar sua jornada.");
    }
  }, [router]);

  useEffect(() => {
    void load();
  }, [load]);

  if (error) return <ErrorMessage message={error} onRetry={load} />;
  if (!journey) return <Loading label="Carregando sua jornada" />;

  const { shortAlternative, interrupted, progress } = journey;
  // Se a missão recomendada é a própria missão interrompida, o card de retomada já basta.
  const sameAsInterrupted =
    interrupted !== null &&
    journey.recommended !== null &&
    interrupted.kind === journey.recommended.kind &&
    interrupted.topicId === journey.recommended.topicId &&
    interrupted.lessonId === journey.recommended.lessonId &&
    interrupted.reviewId === journey.recommended.reviewId;
  const recommended = sameAsInterrupted ? null : journey.recommended;

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-bold sm:text-3xl">Olá, {journey.displayName}</h1>
        {!focus && journey.role && (
          <p className="text-muted">
            {journey.exam?.name} · {journey.role.name}
          </p>
        )}
      </header>

      {startError && <ErrorMessage message={startError} />}

      {interrupted && (
        <section aria-labelledby="retomada" className="rounded-2xl border border-info/40 bg-info-soft p-5">
          <h2 id="retomada" className="text-lg font-semibold text-info">
            Você parou neste desafio. Deseja continuar?
          </h2>
          <p className="mt-1">
            {sessionKindLabel[interrupted.kind]}: <strong>{interrupted.lessonTitle ?? interrupted.topicName}</strong>
          </p>
          <p className="text-sm text-muted">
            {interrupted.answered} de {interrupted.totalQuestions} questões respondidas — nada se perde, você continua de onde parou.
          </p>
          <ButtonLink href={`/missao/${interrupted.sessionId}`} className="mt-4">
            Continuar de onde parei
          </ButtonLink>
        </section>
      )}

      {recommended ? (
        <MissionCard mission={recommended} onStart={() => start(recommended)} busy={busy} />
      ) : sameAsInterrupted ? null : (
        <Card>
          <h2 className="text-xl font-semibold">Você percorreu todo o conteúdo disponível 🎉</h2>
          <p className="mt-2 text-muted">Novas lições e questões serão publicadas em breve. Enquanto isso, explore o mapa.</p>
          <ButtonLink href="/mapa" variant="secondary" className="mt-4">
            Abrir mapa de aprendizagem
          </ButtonLink>
        </Card>
      )}

      {shortAlternative && !sameAsInterrupted && (
        <div className="flex flex-col gap-3 rounded-2xl border border-dashed border-line p-4 sm:flex-row sm:items-center sm:justify-between">
          <p>
            <span className="font-semibold">Pouco tempo?</span>{" "}
            <span className="text-muted">
              {missionTitle(shortAlternative)} · cerca de {shortAlternative.estimatedMinutes} min
            </span>
          </p>
          <Button variant="secondary" onClick={() => start(shortAlternative)} disabled={busy}>
            Fazer missão curta
          </Button>
        </div>
      )}

      {!focus && progress && (
        <div className="grid gap-4 sm:grid-cols-2">
          <Card aria-labelledby="progresso-titulo">
            <h2 id="progresso-titulo" className="font-semibold">
              Edital percorrido
            </h2>
            <p className="mt-2 text-3xl font-bold">{Math.round(progress.coveragePercent)}%</p>
            <ProgressBar value={progress.coveragePercent} label="Percentual do edital percorrido" className="mt-3" />
            <p className="mt-3 text-sm text-muted">
              {progress.topicsStarted} de {progress.topicsTotal} assuntos iniciados · {progress.topicsMastered} com domínio estimado
            </p>
            <dl className="mt-4 grid grid-cols-2 gap-3 text-sm">
              <div>
                <dt className="text-muted">Acertos</dt>
                <dd className="text-lg font-semibold">{progress.attempts ? percent(progress.accuracy) : "—"}</dd>
              </div>
              <div>
                <dt className="text-muted">XP</dt>
                <dd className="text-lg font-semibold">{progress.xp}</dd>
              </div>
            </dl>
            <Link href="/progresso" className="mt-4 inline-block font-semibold text-brand hover:underline">
              Ver progresso detalhado
            </Link>
          </Card>

          <Card aria-labelledby="revisoes-titulo">
            <h2 id="revisoes-titulo" className="font-semibold">
              Revisões
            </h2>
            {journey.upcomingReviews.length === 0 ? (
              <p className="mt-2 text-sm text-muted">Ao concluir uma lição, agendamos revisões no momento certo para fixar o conteúdo.</p>
            ) : (
              <ul className="mt-3 space-y-2">
                {journey.upcomingReviews.map((r) => (
                  <li key={r.reviewId} className="flex items-center justify-between gap-3 text-sm">
                    <span>{r.topicName}</span>
                    <span className={r.due ? "font-semibold text-warning" : "text-muted"}>
                      {r.due ? "pendente" : relativeDay(r.dueAt)}
                    </span>
                  </li>
                ))}
              </ul>
            )}
            <div className="mt-4 flex flex-wrap gap-4">
              <Link href="/revisoes" className="font-semibold text-brand hover:underline">
                Todas as revisões
              </Link>
              <Link href="/mapa" className="font-semibold text-brand hover:underline">
                Mapa de aprendizagem
              </Link>
            </div>
          </Card>
        </div>
      )}
    </div>
  );
}

export default function HomePage() {
  return (
    <UserProvider>
      <AppShell>
        <JourneyView />
      </AppShell>
    </UserProvider>
  );
}
