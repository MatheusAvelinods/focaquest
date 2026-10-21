# PRD — status de implementação

O documento original está em [PRD-FocaQuest-v2.0.pdf](PRD-FocaQuest-v2.0.pdf). Esta página
acompanha o que foi implementado no MVP.

| Seção | Status | Observações |
|---|---|---|
| 2.1 Modelo curricular | ✅ | Exam → ExamRole → SyllabusVersion → Subject → Topic (hierárquico) → LearningObjective → Lesson → Activity. Cobertura por lições/questões calculada por tópico. |
| 2.2 Concurso inicial | ✅ (estrutura) | 14 disciplinas, 43 tópicos. **Conferir com o edital oficial.** |
| 2.3 Priorização curricular | ✅ | 6 estratégias configuráveis, com regras explícitas. |
| 3.1 Minha jornada | ✅ | Missão recomendada com justificativa, retomada, progresso, revisões, mapa. |
| 3.2 Tipos de missão | ✅ | Micro (2 questões), Padrão (até 4), Aprofundada (até 8). Duração indicativa. |
| 3.3 Microlição | ✅ | Objetivo, explicação, exemplo, recuperação, feedback, registro, próximo passo. |
| 3.4 Questões contextualizadas | ✅ | Campo `context`; alternativa correta justificada e incorretas comentadas. |
| 4.1 Estados de aprendizagem | ✅ | Calculados por evidências (respostas e revisões), não por visualização. |
| 4.2 Motor de recomendação | ✅ | Regras 1–5 implementadas e testadas (`RecommendationEngine`). |
| 4.3 Revisão espaçada | ✅ | Escada determinística `ladder-v1` (1, 3, 7, 14, 30, 60 dias); histórico imutável em `review_log`. |
| 5.1 Modo foco | ✅ | Oculta navegação e informações periféricas; uma tarefa por vez. |
| 5.2 Retomada inteligente | ✅ | "Você parou neste desafio. Deseja continuar?" sem refazer questões. |
| 5.3 Metas sem punição | ✅ parcial | Sem perda por ausência; cronômetro opcional; sem notificações no MVP. Sequências de dias: preferência existe, exibição ainda não. |
| 6 Gamificação (MVP) | ✅ | XP por atividade e retomada, progresso curricular, feedback visual, marcos de domínio. XP ≠ domínio. |
| 7.1 Questões objetivas | ✅ | Todos os campos, incluindo revisão e estado de publicação. |
| 7.2 Simulado | ⏳ | Próxima etapa — o banco atual (47 questões) ainda não comporta uma prova de 60. |
| 7.3 Discursiva | ⏳ | Evolução prioritária, fora do MVP. |
| 8 Administração de conteúdo | ✅ | Importação idempotente via Git, versões, fonte, validação, rejeição de incompletas. |
| 9 Qualidade | ✅ | Responsivo, teclado, WCAG 2.2 AA como guia, validação no backend, testes, logs estruturados, health/metrics. |
| 10 Critério de sucesso | ✅ automatizado | Falta o **teste completo com usuário**. |
