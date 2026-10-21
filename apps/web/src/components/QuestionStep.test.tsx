import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import type { Feedback, PublicQuestion } from "@/lib/types";

import { QuestionStep } from "./QuestionStep";

const question: PublicQuestion = {
  id: "q1",
  topicId: "t1",
  context: null,
  statement: "Assinale a frase correta.",
  difficulty: "FACIL",
  source: "inédita",
  options: ["A", "B", "C", "D", "E"].map((l) => ({ label: l, text: `Alternativa ${l}` })),
};

const feedback: Feedback = {
  questionId: "q1",
  selectedOption: "B",
  correctOption: "C",
  correct: false,
  explanation: "Explicação comentada.",
  options: ["A", "B", "C", "D", "E"].map((l) => ({
    label: l,
    text: `Alternativa ${l}`,
    rationale: `Comentário ${l}`,
    correct: l === "C",
  })),
};

describe("QuestionStep", () => {
  it("exige escolher uma alternativa antes de responder e é operável por teclado", async () => {
    const onAnswer = vi.fn().mockResolvedValue(undefined);
    const user = userEvent.setup();
    render(<QuestionStep question={question} feedback={null} number={1} total={3} onAnswer={onAnswer} />);

    const submit = screen.getByRole("button", { name: "Responder" });
    expect(submit).toBeDisabled();
    expect(screen.getAllByRole("radio")).toHaveLength(5);
    expect(screen.queryByText(/Comentário/)).not.toBeInTheDocument();

    await user.click(screen.getByRole("radio", { name: /Alternativa B/ }));
    await user.keyboard("{ArrowDown}");
    expect(screen.getByRole("radio", { name: /Alternativa C/ })).toBeChecked();
    await user.click(submit);
    expect(onAnswer).toHaveBeenCalledWith("C");
  });

  it("mostra o feedback comentado de cada alternativa após a resposta", () => {
    render(<QuestionStep question={question} feedback={feedback} number={1} total={3} onAnswer={vi.fn()} />);

    expect(screen.getByRole("status")).toHaveTextContent("a resposta é a alternativa C");
    expect(screen.getByRole("status")).toHaveTextContent("Explicação comentada.");
    expect(screen.getByText(/Comentário B/)).toBeInTheDocument();
    expect(screen.getByText(/Comentário C/)).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Responder" })).not.toBeInTheDocument();
    screen.getAllByRole("radio").forEach((r) => expect(r).toBeDisabled());
  });
});
