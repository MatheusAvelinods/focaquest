import { Fragment, type ReactNode } from "react";

/**
 * Renderizador mínimo para o texto das lições (subconjunto de Markdown): parágrafos separados
 * por linha em branco, listas com "-" ou "1.", blocos de código entre ``` e **negrito**.
 * Não interpreta HTML — o conteúdo é sempre tratado como texto.
 */
export function renderInline(text: string): ReactNode[] {
  const parts = text.split(/(\*\*[^*]+\*\*)/g).filter(Boolean);
  return parts.map((p, i) =>
    p.startsWith("**") && p.endsWith("**") ? <strong key={i}>{p.slice(2, -2)}</strong> : <Fragment key={i}>{p}</Fragment>,
  );
}

type Block =
  | { kind: "p"; text: string }
  | { kind: "code"; text: string }
  | { kind: "ul"; items: string[] }
  | { kind: "ol"; items: string[] };

export function parseBlocks(source: string): Block[] {
  const blocks: Block[] = [];
  const parts = source.trim().split(/^```[^\n]*$/m);
  parts.forEach((part, i) => {
    if (i % 2 === 1) {
      blocks.push({ kind: "code", text: part.replace(/^\n|\n$/g, "") });
    } else {
      blocks.push(...parseProse(part));
    }
  });
  return blocks;
}

function parseProse(source: string): Block[] {
  const blocks: Block[] = [];
  if (!source.trim()) return blocks;
  for (const chunk of source.trim().split(/\n\s*\n/)) {
    const lines = chunk.split("\n");
    const isUl = lines.every((l, i) => /^\s*- /.test(l) || (i > 0 && /^\s{2,}\S/.test(l)));
    const isOl = lines.every((l, i) => /^\s*\d+\. /.test(l) || (i > 0 && /^\s{2,}\S/.test(l)));
    if (isUl || isOl) {
      const items: string[] = [];
      for (const l of lines) {
        if (/^\s*(- |\d+\. )/.test(l)) items.push(l.replace(/^\s*(- |\d+\. )/, ""));
        else items[items.length - 1] += " " + l.trim();
      }
      blocks.push(isUl ? { kind: "ul", items } : { kind: "ol", items });
    } else {
      blocks.push({ kind: "p", text: lines.map((l) => l.trim()).join(" ") });
    }
  }
  return blocks;
}

export function Markdown({ source, className }: { source: string; className?: string }) {
  return (
    <div className={className}>
      {parseBlocks(source).map((b, i) => {
        if (b.kind === "ul")
          return (
            <ul key={i} className="my-3 list-disc space-y-1 pl-6">
              {b.items.map((it, j) => (
                <li key={j}>{renderInline(it)}</li>
              ))}
            </ul>
          );
        if (b.kind === "ol")
          return (
            <ol key={i} className="my-3 list-decimal space-y-1 pl-6">
              {b.items.map((it, j) => (
                <li key={j}>{renderInline(it)}</li>
              ))}
            </ol>
          );
        if (b.kind === "code")
          return (
            <pre key={i} className="my-3 overflow-x-auto rounded-xl bg-surface-muted p-4 font-mono text-sm leading-relaxed">
              <code>{b.text}</code>
            </pre>
          );
        return (
          <p key={i} className="my-3">
            {renderInline(b.text)}
          </p>
        );
      })}
    </div>
  );
}
