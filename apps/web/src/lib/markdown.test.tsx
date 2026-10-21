import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { Markdown, parseBlocks } from "./markdown";

describe("parseBlocks", () => {
  it("separa parágrafos e listas", () => {
    const blocks = parseBlocks("Intro **forte**.\n\n- um\n- dois\n  continua\n\n1. primeiro\n2. segundo");
    expect(blocks.map((b) => b.kind)).toEqual(["p", "ul", "ol"]);
    expect(blocks[1]).toEqual({ kind: "ul", items: ["um", "dois continua"] });
  });
});

describe("Markdown", () => {
  it("renderiza negrito e listas", () => {
    render(<Markdown source={"A **crase** é uma fusão.\n\n- a + a = à"} />);
    expect(screen.getByText("crase").tagName).toBe("STRONG");
    expect(screen.getByRole("listitem")).toHaveTextContent("a + a = à");
  });

  it("nunca interpreta HTML do conteúdo", () => {
    const { container } = render(<Markdown source={'<img src=x onerror="alert(1)"> texto'} />);
    expect(container.querySelector("img")).toBeNull();
    expect(container).toHaveTextContent('<img src=x onerror="alert(1)"> texto');
  });
});

describe("parágrafos e código", () => {
  it("une quebras de linha de um parágrafo e preserva blocos de código", () => {
    const blocks = parseBlocks("linha um\nlinha dois\n\n```sql\nSELECT 1\n  FROM t;\n```\n\nfim");
    expect(blocks).toEqual([
      { kind: "p", text: "linha um linha dois" },
      { kind: "code", text: "SELECT 1\n  FROM t;" },
      { kind: "p", text: "fim" },
    ]);
  });
});
