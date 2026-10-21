# ADR 0003 — Revisão espaçada com escada determinística

**Status:** aceito · 2026-10-21

## Contexto
O PRD (§4.3) pede um escalonador determinístico e testável, cuja política possa mudar sem perder
o histórico de tentativas.

## Decisão
Interface `ReviewPolicy` com a implementação `ladder-v1`: intervalos de 1, 3, 7, 14, 30 e 60 dias.
Pontuação ≥ 80% sobe um degrau (e conta como revisão bem-sucedida); entre 50% e 80% mantém;
abaixo de 50% volta ao início. Toda revisão gera um `review_log` imutável com pontuação e degraus.

## Consequências
- Comportamento previsível e coberto por testes parametrizados.
- Algoritmos adaptativos (SM-2, FSRS) podem ser adotados como nova `ReviewPolicy`, recalculando
  agendas a partir de `review_log` e `answer_attempt`.
