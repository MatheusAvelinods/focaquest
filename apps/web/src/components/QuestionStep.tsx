"use client";

import { useEffect, useRef, useState, type FormEvent } from "react";

import { Markdown } from "@/lib/markdown";
import type { Feedback, PublicQuestion } from "@/lib/types";

import { Button } from "./ui";

interface Props {
  question: PublicQuestion;
  feedback: Feedback | null;
  number: number;
  total: number;
  onAnswer: (option: string) => Promise<void>;
}

/**
 * Questão objetiva acessível: alternativas são radios nativos (navegáveis por teclado) e o
 * feedback comentado é anunciado por leitores de tela assim que aparece.
 */
export function QuestionStep({ question, feedback, number, total, onAnswer }: Props) {
  const [selected, setSelected] = useState<string>(feedback?.selectedOption ?? "");
  const [busy, setBusy] = useState(false);
  const feedbackRef = useRef<HTMLDivElement>(null);
  const answered = feedback !== null;

  useEffect(() => {
    setSelected(feedback?.selectedOption ?? "");
  }, [question.id, feedback?.selectedOption]);

  useEffect(() => {
    if (feedback) feedbackRef.current?.focus();
  }, [feedback]);

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!selected || answered) return;
    setBusy(true);
    try {
      await onAnswer(selected);
    } finally {
      setBusy(false);
    }
  }

  const optionState = (label: string) => {
    if (!feedback) return "";
    const opt = feedback.options.find((o) => o.label === label);
    if (opt?.correct) return "border-success bg-success-soft";
    if (label === feedback.selectedOption) return "border-danger bg-danger-soft";
    return "opacity-80";
  };

  return (
    <form onSubmit={submit} aria-describedby={`enunciado-${question.id}`}>
      <p className="text-sm font-semibold text-muted">
        Questão {number} de {total}
      </p>
      {question.context && (
        <div className="reading mt-3 rounded-xl border border-line bg-surface-muted/60 px-4 py-1">
          <Markdown source={question.context} />
        </div>
      )}
      <fieldset className="mt-4">
        <legend id={`enunciado-${question.id}`} className="reading text-lg font-medium">
          <Markdown source={question.statement} />
        </legend>
        <div className="mt-4 space-y-2">
          {question.options.map((o) => {
            const fb = feedback?.options.find((f) => f.label === o.label);
            return (
              <label
                key={o.label}
                className={`flex cursor-pointer items-start gap-3 rounded-xl border border-line p-4 has-[:checked]:border-brand ${
                  answered ? "cursor-default" : "hover:bg-surface-muted"
                } ${optionState(o.label)}`}
              >
                <input
                  type="radio"
                  name={`q-${question.id}`}
                  value={o.label}
                  checked={selected === o.label}
                  disabled={answered}
                  onChange={() => setSelected(o.label)}
                  className="mt-1.5 h-4 w-4 shrink-0 accent-[var(--brand)]"
                />
                <span className="min-w-0">
                  <span className="font-semibold">{o.label})</span> <span className="break-words">{o.text}</span>
                  {fb && (
                    <span className="mt-2 block text-sm text-muted">
                      {fb.correct && <span className="mr-1 font-semibold text-success">✓ Correta.</span>}
                      {!fb.correct && fb.label === feedback?.selectedOption && (
                        <span className="mr-1 font-semibold text-danger">✗ Sua resposta.</span>
                      )}
                      {fb.rationale}
                    </span>
                  )}
                </span>
              </label>
            );
          })}
        </div>
      </fieldset>

      {!answered && (
        <Button type="submit" disabled={!selected || busy} className="mt-5 w-full sm:w-auto">
          {busy ? "Enviando…" : "Responder"}
        </Button>
      )}

      {feedback && (
        <div
          ref={feedbackRef}
          tabIndex={-1}
          role="status"
          aria-live="polite"
          className={`mt-5 rounded-2xl border p-5 ${
            feedback.correct ? "border-success/40 bg-success-soft" : "border-warning/40 bg-warning-soft"
          }`}
        >
          <p className={`text-lg font-semibold ${feedback.correct ? "text-success" : "text-warning"}`}>
            {feedback.correct ? "Isso mesmo!" : `Ainda não — a resposta é a alternativa ${feedback.correctOption}.`}
          </p>
          <Markdown source={feedback.explanation} className="reading" />
          {!feedback.correct && (
            <p className="mt-2 text-sm text-muted">Errar faz parte: este assunto voltará em uma revisão para você fixar.</p>
          )}
        </div>
      )}
    </form>
  );
}
