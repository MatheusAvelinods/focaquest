# ADR 0002 — Conteúdo versionado no Git, sem painel administrativo

**Status:** aceito · 2026-10-21

## Contexto
O PRD (§8) dispensa painel administrativo na primeira versão, mas exige importação idempotente,
versões, rastreabilidade de fonte e rejeição de questões incompletas.

## Decisão
Conteúdo em YAML dentro de `content/`, revisado por pull request. A API valida o pacote inteiro
e importa na inicialização; o CI roda a mesma validação em todo PR. Itens usam ids estáveis;
alterações geram nova revisão; remoções marcam o item como `RETIRED`.

## Consequências
- Revisão editorial com histórico, diff e autoria do próprio Git.
- Editores precisam de familiaridade mínima com YAML; um painel pode ser construído depois
  sobre o mesmo importador e validador.
