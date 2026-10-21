# ADR 0001 — Monólito modular

**Status:** aceito · 2026-10-21

## Contexto
O MVP precisa validar um ciclo completo de aprendizagem com uma equipe pequena. Microsserviços
trariam custo operacional (deploys, rede, consistência distribuída) sem benefício nesta fase.

## Decisão
Uma única aplicação Spring Boot, dividida em pacotes de domínio (`auth`, `catalog`, `questions`,
`learning`, `progress`, `review`, `content`, `shared`) com dependências em uma só direção. Regras
pedagógicas ficam em classes puras, sem dependência de framework.

## Consequências
- Deploy, transações e testes de integração simples.
- Fronteiras claras permitem extrair um módulo no futuro, se houver necessidade real.
- A direção das dependências precisa ser mantida por revisão de código (candidato a ArchUnit).
