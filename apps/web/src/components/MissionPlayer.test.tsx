import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import type { Session } from "@/lib/types";

const push = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ push, replace: vi.fn() }) }));

const apiMock = vi.hoisted(() => ({
  updateSession: vi.fn(),
  answer: vi.fn(),
  completeReview: vi.fn(),
}));
vi.mock("@/lib/api", async (orig) => ({ ...(await orig<typeof import("@/lib/api")>()), api: apiMock }));

import { MissionPlayer } from "./MissionPlayer";

const options = ["A", "B", "C", "D", "E"].map((l) => ({ label: l, text: `Opção ${l}` }));

function session(): Session {
  return {
    id: "s1",
    kind: "LESSON",
    missionType: "MICRO",
    status: "IN_PROGRESS",
    currentStep: 0,
    startedAt: "2026-10-21T12:00:00Z",
    lastActivityAt: "2026-10-21T12:00:00Z",
    topic: { id: "t1", name: "Crase", subjectId: "pt", subjectName: "Língua Portuguesa" },
    lesson: {
      id: "l1",
      title: "Crase sem decoreba",
      objective: "Decidir se há crase.",
      explanation: "Crase é **fusão**.",
      example: "Fui à reunião.",
      summary: "Troque pelo masculino.",
      learningObjectives: [],
    },
    reviewId: null,
    steps: [
      { index: 0, type: "OBJECTIVE", question: null, feedback: null },
      { index: 1, type: "EXPLANATION", question: null, feedback: null },
      { index: 2, type: "EXAMPLE", question: null, feedback: null },
      {
        index: 3,
        type: "QUESTION",
        question: { id: "q1", topicId: "t1", context: null, statement: "Qual?", difficulty: "FACIL", source: "x", options },
        feedback: null,
      },
      { index: 4, type: "SUMMARY", question: null, feedback: null },
    ],
    answeredCount: 0,
    totalQuestions: 1,
    result: null,
  };
}

describe("MissionPlayer", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    apiMock.updateSession.mockImplementation(async (_id: string, body: { currentStep?: number }) => ({
      ...session(),
      currentStep: body.currentStep ?? 0,
    }));
  });

  it("conduz a missão passo a passo até a conclusão", async () => {
    const user = userEvent.setup();
    apiMock.answer.mockResolvedValue({
      alreadyAnswered: false,
      xpEarned: 5,
      topicState: "LEARNING",
      currentStep: 3,
      feedback: {
        questionId: "q1",
        selectedOption: "A",
        correctOption: "A",
        correct: true,
        explanation: "Porque sim.",
        options: options.map((o) => ({ ...o, rationale: `R ${o.label}`, correct: o.label === "A" })),
      },
    });

    render(<MissionPlayer initial={session()} focusMode={false} showTimer={false} />);
    expect(screen.getByText("Decidir se há crase.")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /Começar/ }));
    expect(screen.getByText("fusão").tagName).toBe("STRONG");
    await user.click(screen.getByRole("button", { name: /Continuar/ }));
    await user.click(screen.getByRole("button", { name: /Praticar/ }));

    // Não dá para avançar sem responder.
    expect(screen.getByRole("button", { name: /Continuar/ })).toBeDisabled();
    await user.click(screen.getByRole("radio", { name: /Opção A/ }));
    await user.click(screen.getByRole("button", { name: "Responder" }));
    expect(await screen.findByText("Isso mesmo!")).toBeInTheDocument();
    expect(apiMock.answer).toHaveBeenCalledWith("s1", "q1", "A");

    await user.click(screen.getByRole("button", { name: /Continuar/ }));
    apiMock.updateSession.mockResolvedValueOnce({
      ...session(),
      status: "COMPLETED",
      result: { correct: 1, total: 1, score: 1, xpAwarded: 17, topicState: "IN_REVIEW", masteryMilestone: false, nextReviewAt: "2026-10-22T12:00:00Z" },
    });
    await user.click(screen.getByRole("button", { name: "Concluir missão" }));
    expect(await screen.findByText("Missão concluída!")).toBeInTheDocument();
    expect(screen.getByText("+17")).toBeInTheDocument();
    expect(apiMock.updateSession).toHaveBeenLastCalledWith("s1", { action: "COMPLETE" });
  });

  it("pausar salva e volta para a jornada", async () => {
    const user = userEvent.setup();
    render(<MissionPlayer initial={session()} focusMode={true} showTimer={false} />);
    await user.click(screen.getByRole("button", { name: /Pausar/ }));
    await waitFor(() => expect(push).toHaveBeenCalledWith("/"));
    expect(apiMock.updateSession).toHaveBeenCalledWith("s1", { action: "PAUSE" });
  });

  it("retoma no passo em que o estudante parou", () => {
    render(<MissionPlayer initial={{ ...session(), currentStep: 3 }} focusMode={false} showTimer={false} />);
    expect(screen.getByText("Questão 1 de 1")).toBeInTheDocument();
  });
});
