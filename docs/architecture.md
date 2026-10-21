# Arquitetura

## Visão geral

```
Navegador (PWA) ──► Next.js (apps/web) ──/api/*──► Spring Boot (apps/api) ──► PostgreSQL
                     mesma origem                    monólito modular         Flyway
                                                        ▲
                                         content/ (YAML no Git) importado na inicialização
```

O navegador só conversa com a origem do frontend; o Next.js repassa `/api/*` para a API. Assim
o token de sessão vive em um cookie `httpOnly` + `SameSite=Strict`, sem CORS e inacessível a
JavaScript (ver [ADR 0004](adr/0004-autenticacao-cookie-jwt.md)).

## Módulos da API (`com.focaquest.*`)

| Módulo | Responsabilidade | Depende de |
|---|---|---|
| `shared` | Enums comuns, erros (Problem Details), relógio injetável, usuário atual | — |
| `auth` | Cadastro, login, JWT, perfil e preferências | `catalog` |
| `catalog` | Concurso, cargo, edital, disciplinas, tópicos, objetivos; cobertura | — |
| `questions` | Questões, alternativas, revisões editoriais; projeções públicas sem gabarito | — |
| `review` | Agenda de revisões e política de espaçamento (`ReviewPolicy`) | — |
| `progress` | Estado por tópico (`MasteryCalculator`), XP, resumo de progresso | `catalog`, `review` |
| `learning` | Lições, sessões de estudo, respostas, seleção de questões, recomendação, jornada | todos acima |
| `content` | Carga, validação e importação idempotente do conteúdo versionado | repositórios dos demais |

As dependências apontam sempre na mesma direção (sem ciclos): `learning` orquestra; `review` e
`questions` não conhecem ninguém. O catálogo lê contagens de `lesson`/`question` por SQL nativo
para calcular cobertura sem depender do módulo `learning`.

### Separação entre conteúdo, regras pedagógicas e apresentação (PRD §9)

- **Conteúdo**: arquivos em `content/`, nunca no código.
- **Regras pedagógicas**: classes puras e determinísticas, testadas isoladamente —
  `RecommendationEngine`, `QuestionSelector`, `MasteryCalculator`, `LadderReviewPolicy`. Todas
  recebem o instante atual como parâmetro ou via `Clock`.
- **Apresentação**: `apps/web`, que só consome a API.

## Fluxo de uma missão

1. `GET /me/journey` — o `RecommendationService` reúne evidências (estados dos tópicos,
   revisões, últimas sessões) e o `RecommendationEngine` decide a missão e a alternativa curta.
2. `POST /study-sessions` — o `QuestionSelector` escolhe e **congela** as questões da sessão. Se a
   mesma missão já está aberta, ela é devolvida (retomada em vez de duplicata).
3. `GET /study-sessions/{id}` — passos da missão; gabarito e explicação só aparecem para
   questões já respondidas.
4. `POST /study-sessions/{id}/answers` — registra a tentativa (a primeira resposta vale),
   atualiza o progresso do tópico e concede XP.
5. `PATCH /study-sessions/{id}` — `PAUSE`, `RESUME`, avanço de passo ou `COMPLETE`. Ao concluir
   uma lição, agenda a primeira revisão; ao concluir uma revisão, aplica a política de
   espaçamento e registra o `review_log`.

## Regras do motor de recomendação (PRD §4.2)

Ordem padrão da fila de missões:

1. Revisões vencidas, da mais atrasada para a mais recente.
2. Se as duas últimas missões concluídas foram revisões e há conteúdo novo, o conteúdo novo
   vem primeiro (alternância).
3. Tópicos em **Precisa de reforço**, do menor para o maior desempenho.
4. Próxima lição ainda não concluída, ordenada pela estratégia escolhida.
5. Revisões futuras (antecipação), quando não há mais nada.

A estratégia `LOW_PERFORMANCE` põe o reforço antes das revisões; `REVIEW_FIRST` antecipa
revisões antes do conteúdo novo. A primeira missão da fila é a recomendada, no tipo preferido
pelo estudante; a **alternativa curta** é a mesma missão em formato Micro (ou, se a preferência
já é Micro, a próxima da fila).

## Estados de aprendizagem (PRD §4.1)

Calculados por `MasteryCalculator` a partir das evidências, com janela das 6 respostas mais recentes:

| Estado | Condição |
|---|---|
| Não iniciado | nenhuma resposta e nenhuma lição concluída |
| Precisa de reforço | ≥ 2 respostas recentes e acerto recente < 60% |
| Domínio estimado | ≥ 5 respostas, acerto recente ≥ 80% e ≥ 2 revisões espaçadas bem-sucedidas |
| Em revisão | lição concluída e revisão agendada |
| Em aprendizagem | demais casos |

## Observabilidade

- Logs estruturados em eventos de domínio (`session.start`, `session.complete`, `content.import`).
- `GET /actuator/health` (com probes de liveness/readiness), `/actuator/metrics`.
- Erros no formato Problem Details (RFC 9457), sem stack trace na resposta.
