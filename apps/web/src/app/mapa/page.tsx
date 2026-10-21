"use client";

import { useCallback, useEffect, useState } from "react";

import { AppShell } from "@/components/AppShell";
import { Button, Card, ErrorMessage, Loading, ProgressBar, StateBadge } from "@/components/ui";
import { api, ApiError } from "@/lib/api";
import { percent } from "@/lib/labels";
import type { ProgressSummary, Syllabus, Topic, TopicProgress } from "@/lib/types";
import { UserProvider, useUser } from "@/lib/user";
import { useStartMission } from "@/lib/useStartMission";

function LearningMap() {
  const { user } = useUser();
  const [syllabus, setSyllabus] = useState<Syllabus | null>(null);
  const [progress, setProgress] = useState<ProgressSummary | null>(null);
  const [error, setError] = useState<string | null>(null);
  const { start, busy, error: startError } = useStartMission();

  const load = useCallback(async () => {
    if (!user?.selectedRoleId) return;
    try {
      const [s, p] = await Promise.all([api.syllabus(user.selectedRoleId), api.progress()]);
      setSyllabus(s);
      setProgress(p);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Não foi possível carregar o mapa.");
    }
  }, [user?.selectedRoleId]);

  useEffect(() => {
    void load();
  }, [load]);

  if (error) return <ErrorMessage message={error} onRetry={load} />;
  if (!syllabus || !progress) return <Loading label="Carregando o mapa de aprendizagem" />;

  const byTopic = new Map<string, TopicProgress>(progress.topics.map((t) => [t.topicId, t]));

  async function study(topic: Topic) {
    const p = byTopic.get(topic.id);
    const missionType = user?.preferences.preferredMissionType ?? "STANDARD";
    const firstLesson = topic.lessons[0];
    // Assunto novo: estudar a lição. Já estudado: praticar (reforço com questões erradas e inéditas).
    if (firstLesson && (!p || p.state === "NOT_STARTED" || p.state === "LEARNING")) {
      return start({ kind: "LESSON", lessonId: firstLesson.id, missionType });
    }
    return start({ kind: "REINFORCEMENT", topicId: topic.id, missionType });
  }

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-bold sm:text-3xl">Mapa de aprendizagem</h1>
        <p className="text-muted">
          {syllabus.role.name} · edital versão {syllabus.version}
        </p>
        <p className="mt-2 text-sm text-muted">
          O estado de cada assunto vem das suas respostas e revisões — não apenas das lições vistas.
        </p>
      </header>
      {startError && <ErrorMessage message={startError} />}

      {(["GERAIS", "ESPECIFICOS"] as const).map((area) => (
        <section key={area} aria-labelledby={`area-${area}`} className="space-y-4">
          <h2 id={`area-${area}`} className="text-lg font-semibold">
            {area === "GERAIS" ? "Conhecimentos Gerais" : "Conhecimentos Específicos"}
          </h2>
          {syllabus.subjects
            .filter((s) => s.area === area)
            .map((subject) => {
              const sp = progress.subjects.find((x) => x.subjectId === subject.id);
              return (
                <Card key={subject.id} as="article">
                  <div className="flex flex-col gap-2 sm:flex-row sm:items-baseline sm:justify-between">
                    <h3 className="text-lg font-semibold">{subject.name}</h3>
                    <p className="text-sm text-muted">
                      {sp ? `${sp.topicsStarted}/${sp.topicsTotal} assuntos iniciados` : null}
                      {subject.examQuestionCount ? ` · ${subject.examQuestionCount} questões na prova` : ""}
                    </p>
                  </div>
                  {sp && (
                    <ProgressBar
                      value={sp.topicsTotal ? (sp.topicsStarted / sp.topicsTotal) * 100 : 0}
                      label={`Progresso em ${subject.name}`}
                      className="mt-3"
                    />
                  )}
                  <ul className="mt-4 divide-y divide-line">
                    {subject.topics.map((t) => {
                      const tp = byTopic.get(t.id);
                      const available = t.lessonCount > 0 || t.questionCount > 0;
                      return (
                        <li key={t.id} className="flex flex-col gap-2 py-3 sm:flex-row sm:items-center sm:justify-between" style={{ paddingLeft: `${t.depth * 1.25}rem` }}>
                          <div className="min-w-0">
                            <p className="font-medium">
                              {t.editalCode && <span className="text-muted">{t.editalCode} </span>}
                              {t.name}
                            </p>
                            <div className="mt-1 flex flex-wrap items-center gap-2 text-sm text-muted">
                              <StateBadge state={tp?.state ?? "NOT_STARTED"} />
                              {tp && tp.attempts > 0 && <span>{percent(tp.accuracy)} de acerto em {tp.attempts} respostas</span>}
                              {!available && <span>Conteúdo em preparação</span>}
                            </div>
                          </div>
                          {available && (
                            <Button variant="secondary" className="shrink-0" disabled={busy} onClick={() => study(t)}>
                              {tp && tp.state !== "NOT_STARTED" && tp.state !== "LEARNING" ? "Praticar" : "Estudar"}
                            </Button>
                          )}
                        </li>
                      );
                    })}
                  </ul>
                </Card>
              );
            })}
        </section>
      ))}
      {syllabus.sourceRef && <p className="text-xs text-muted">Fonte: {syllabus.sourceRef}</p>}
    </div>
  );
}

export default function MapaPage() {
  return (
    <UserProvider>
      <AppShell>
        <LearningMap />
      </AppShell>
    </UserProvider>
  );
}
