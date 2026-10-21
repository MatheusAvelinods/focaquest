"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { api, ApiError } from "@/lib/api";
import { Markdown } from "@/lib/markdown";
import { missionTypeLabel, relativeDay, sessionKindLabel, topicStateLabel } from "@/lib/labels";
import type { Feedback, Session } from "@/lib/types";

import { QuestionStep } from "./QuestionStep";
import { Button, ButtonLink, ErrorMessage, ProgressBar } from "./ui";

interface Props {
  initial: Session;
  focusMode: boolean;
  showTimer: boolean;
}

/** Conduz a missão passo a passo: uma tarefa por vez, com progresso salvo a cada ação. */
export function MissionPlayer({ initial, focusMode, showTimer }: Props) {
  const router = useRouter();
  const [session, setSession] = useState(initial);
  const [step, setStep] = useState(Math.min(initial.currentStep, initial.steps.length - 1));
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const current = session.steps[step];
  const questionSteps = session.steps.filter((s) => s.type === "QUESTION");
  const questionNumber = questionSteps.findIndex((s) => s.index === current.index) + 1;
  const allAnswered = questionSteps.every((s) => s.feedback !== null);
  const done = session.status === "COMPLETED";

  async function goTo(index: number) {
    setError(null);
    setStep(index);
    window.scrollTo({ top: 0 });
    if (!done && index > session.currentStep) {
      try {
        const updated = await api.updateSession(session.id, { currentStep: index });
        setSession((s) => ({ ...s, currentStep: updated.currentStep, status: updated.status }));
      } catch {
        // O avanço visual não depende do servidor; a próxima ação sincroniza o passo.
      }
    }
  }

  async function answer(option: string) {
    if (!current.question) return;
    try {
      const res = await api.answer(session.id, current.question.id, option);
      setSession((s) => ({
        ...s,
        answeredCount: res.alreadyAnswered ? s.answeredCount : s.answeredCount + 1,
        steps: s.steps.map((st) => (st.index === current.index ? { ...st, feedback: res.feedback as Feedback } : st)),
      }));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Não foi possível registrar a resposta.");
    }
  }

  async function pause() {
    setBusy(true);
    try {
      await api.updateSession(session.id, { action: "PAUSE" });
    } catch {
      // Mesmo sem confirmação, as respostas já enviadas estão salvas.
    }
    router.push("/");
  }

  async function complete() {
    setBusy(true);
    setError(null);
    try {
      const updated =
        session.kind === "REVIEW" && session.reviewId
          ? await api.completeReview(session.reviewId, session.id)
          : await api.updateSession(session.id, { action: "COMPLETE" });
      setSession(updated);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Não foi possível concluir a missão.");
    } finally {
      setBusy(false);
    }
  }

  if (done && session.result) {
    const r = session.result;
    return (
      <section aria-labelledby="resultado" className="mx-auto max-w-2xl space-y-5 text-center">
        <p className="text-5xl" aria-hidden>
          {r.masteryMilestone ? "🏅" : r.score >= 0.8 ? "🌟" : "💪"}
        </p>
        <h1 id="resultado" className="text-2xl font-bold sm:text-3xl">
          {r.masteryMilestone ? "Marco de domínio alcançado!" : "Missão concluída!"}
        </h1>
        <p className="text-lg">
          Você acertou <strong>{r.correct}</strong> de <strong>{r.total}</strong> em {session.topic.name}.
        </p>
        <dl className="mx-auto grid max-w-md grid-cols-2 gap-3 text-left">
          <div className="rounded-2xl border border-line bg-surface p-4">
            <dt className="text-sm text-muted">XP ganho</dt>
            <dd className="text-2xl font-bold text-brand-strong">+{r.xpAwarded}</dd>
          </div>
          <div className="rounded-2xl border border-line bg-surface p-4">
            <dt className="text-sm text-muted">Situação do assunto</dt>
            <dd className="font-semibold">{topicStateLabel[r.topicState]}</dd>
          </div>
        </dl>
        {r.nextReviewAt && (
          <p className="text-muted">Próxima revisão deste assunto: {relativeDay(r.nextReviewAt)}.</p>
        )}
        {r.score < 0.6 && (
          <p className="text-muted">Vamos reforçar este assunto em breve — repetir com espaçamento é o que fixa o conteúdo.</p>
        )}
        <div className="flex flex-col justify-center gap-3 sm:flex-row">
          <ButtonLink href="/">Próxima missão</ButtonLink>
          {!focusMode && (
            <ButtonLink href="/mapa" variant="secondary">
              Ver mapa
            </ButtonLink>
          )}
        </div>
      </section>
    );
  }

  const lesson = session.lesson;
  const total = session.steps.length;
  const isLast = step === total - 1;
  const canAdvance = current.type !== "QUESTION" || current.feedback !== null;

  return (
    <div className="mx-auto max-w-3xl">
      <div className="mb-6 flex items-start justify-between gap-3">
        <div className="min-w-0">
          {!focusMode && (
            <p className="text-sm text-muted">
              {session.topic.subjectName} · {sessionKindLabel[session.kind]} · {missionTypeLabel[session.missionType]}
            </p>
          )}
          <h1 className="text-xl font-bold sm:text-2xl">{lesson?.title ?? session.topic.name}</h1>
        </div>
        <div className="flex shrink-0 items-center gap-3">
          {showTimer && <Timer since={session.startedAt} />}
          <Button variant="secondary" onClick={pause} disabled={busy} aria-label="Pausar e salvar progresso">
            Pausar
          </Button>
        </div>
      </div>

      <ProgressBar value={((step + 1) / total) * 100} label={`Passo ${step + 1} de ${total}`} />
      <p className="mt-2 text-sm text-muted">
        Passo {step + 1} de {total}
      </p>

      <div className="mt-6 rounded-2xl border border-line bg-surface p-5 sm:p-8">
        {current.type === "OBJECTIVE" && lesson && (
          <div>
            <h2 className="text-lg font-semibold text-brand-strong">Objetivo</h2>
            <p className="reading mt-2 text-lg">{lesson.objective}</p>
            {lesson.learningObjectives.length > 0 && (
              <>
                <h3 className="mt-5 font-semibold">No edital</h3>
                <ul className="mt-2 list-disc space-y-1 pl-6 text-muted">
                  {lesson.learningObjectives.map((o) => (
                    <li key={o}>{o}</li>
                  ))}
                </ul>
              </>
            )}
          </div>
        )}
        {current.type === "EXPLANATION" && lesson && (
          <div>
            <h2 className="text-lg font-semibold text-brand-strong">Entenda o conceito</h2>
            <Markdown source={lesson.explanation} className="reading" />
          </div>
        )}
        {current.type === "EXAMPLE" && lesson && (
          <div>
            <h2 className="text-lg font-semibold text-brand-strong">Exemplo aplicado</h2>
            <Markdown source={lesson.example} className="reading" />
          </div>
        )}
        {current.type === "QUESTION" && current.question && (
          <QuestionStep
            question={current.question}
            feedback={current.feedback}
            number={questionNumber}
            total={questionSteps.length}
            onAnswer={answer}
          />
        )}
        {current.type === "SUMMARY" && (
          <div>
            <h2 className="text-lg font-semibold text-brand-strong">Para guardar</h2>
            <p className="reading mt-2 text-lg">
              {lesson?.summary ?? `Você revisou ${session.topic.name}. Conclua para registrar o resultado e agendar a próxima revisão.`}
            </p>
            {!allAnswered && (
              <p className="mt-4 text-warning">Ainda há questões sem resposta. Volte a elas para concluir a missão.</p>
            )}
          </div>
        )}
      </div>

      {error && (
        <div className="mt-4">
          <ErrorMessage message={error} />
        </div>
      )}

      <div className="mt-6 flex items-center justify-between gap-3">
        <Button variant="ghost" onClick={() => goTo(step - 1)} disabled={step === 0}>
          ← Voltar
        </Button>
        {isLast ? (
          <Button onClick={complete} disabled={!allAnswered || busy}>
            {busy ? "Concluindo…" : "Concluir missão"}
          </Button>
        ) : (
          <Button onClick={() => goTo(step + 1)} disabled={!canAdvance}>
            {current.type === "EXAMPLE" ? "Praticar →" : current.type === "OBJECTIVE" ? "Começar →" : "Continuar →"}
          </Button>
        )}
      </div>
      {!focusMode && (
        <p className="mt-6 text-center text-sm text-muted">
          Pode sair quando quiser: o progresso fica salvo. <Link href="/" className="text-brand hover:underline">Voltar à jornada</Link>
        </p>
      )}
    </div>
  );
}

function Timer({ since }: { since: string }) {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(id);
  }, []);
  const secs = Math.max(0, Math.floor((now - new Date(since).getTime()) / 1000));
  const mm = String(Math.floor(secs / 60)).padStart(2, "0");
  const ss = String(secs % 60).padStart(2, "0");
  return (
    <span className="font-mono text-sm text-muted" aria-label={`Tempo de estudo: ${mm} minutos e ${ss} segundos`}>
      {mm}:{ss}
    </span>
  );
}
