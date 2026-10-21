# Modelo de dados

Schema definido em `apps/api/src/main/resources/db/migration` (Flyway). Itens curriculares usam
**identificadores estáveis em texto**, definidos nos arquivos de conteúdo (ex.: `bd.normalizacao`,
`q.bd.norm.01`); dados de estudantes usam UUID.

## Catálogo e conteúdo

| Tabela | Descrição |
|---|---|
| `exam` | Concurso (banca, ano, fonte oficial, data da revisão editorial) |
| `exam_role` | Cargo do concurso |
| `syllabus_version` | Versão do edital por cargo; apenas uma `current` |
| `subject` | Agrupamento pedagógico (área GERAIS/ESPECIFICOS, nº de questões na prova) |
| `topic` | Item do edital, hierárquico (`parent_id`), com código do edital e fonte |
| `learning_objective` | Objetivos de aprendizagem do tópico |
| `lesson` | Microlição: objetivo, explicação, exemplo, resumo, estado de publicação |
| `activity` | Atividade de recuperação da lição (aponta para uma questão) |
| `question` / `question_option` | Questão objetiva, gabarito, explicação, comentário por alternativa, dificuldade, fonte, autoria, `revision`, `status` |
| `question_revision` | Histórico de revisões editoriais (hash e versão do conteúdo) |
| `content_import` | Registro de cada importação (versão, criados, alterados, inalterados) |

## Estudante

| Tabela | Descrição |
|---|---|
| `app_user` | Conta, cargo selecionado e preferências (estratégia, duração, modo foco, animações, cronômetro…) |
| `study_session` | Missão: tipo (lição/reforço/revisão), duração, status, passo atual |
| `session_question` | Questões congeladas da sessão, em ordem — permitem retomar sem refazer |
| `answer_attempt` | Evidência: resposta, acerto, revisão da questão respondida; única por sessão+questão |
| `user_topic_progress` | Estado do tópico, contadores e janela de resultados recentes; data do marco de domínio |
| `review_schedule` | Próxima revisão por tópico: degrau, intervalo, vencimento, revisões bem-sucedidas, política |
| `review_log` | Histórico imutável das revisões (pontuação, degrau anterior e novo, política) |
| `xp_event` | Eventos de XP, únicos por (motivo, referência) — XP é separado de domínio |

## Decisões

- **Retirar em vez de apagar**: conteúdo removido dos arquivos vira `RETIRED`; tentativas antigas
  continuam válidas e apontam para a revisão respondida (`question_revision`).
- **Trocar a política de revisão sem perder histórico**: `review_log` guarda as evidências brutas;
  uma nova política pode recalcular agendas a partir delas.
