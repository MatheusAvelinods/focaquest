# Formato do conteúdo

```
content/tjpb-2026/
  syllabus/exam.yaml          concurso, cargos e versão do edital
  syllabus/subjects/*.yaml    uma disciplina por arquivo, com a árvore de tópicos
  lessons/*.yaml              uma microlição por arquivo
  questions/*.yaml            listas de questões
```

## Questão

```yaml
questions:
  - id: q.bd.norm.01            # estável: nunca reutilize um id
    topic: bd.normalizacao      # precisa existir no edital
    difficulty: MEDIA           # FACIL | MEDIA | DIFICIL
    boardStyle: FGV             # FGV exige exatamente A, B, C, D, E, em ordem
    source: Questão inédita — Equipe FocaQuest (estilo FGV)
    author: Equipe FocaQuest
    status: PUBLISHED           # DRAFT | PUBLISHED | RETIRED
    context: |                  # opcional — situação-problema (aceita ``` para código)
      ...
    statement: Assinale a alternativa que...
    options:
      - label: A
        text: ...
        rationale: Por que esta alternativa está certa ou errada   # obrigatório em todas
    correct: B
    explanation: Explicação geral, exibida após a resposta.
```

## Lição

```yaml
id: l.bd.normalizacao
topic: bd.normalizacao
title: Normalização — da tabela única às formas normais
status: PUBLISHED
position: 1
reviewedAt: 2026-10-21
objective: ...
explanation: |      # Markdown simples: parágrafos, listas, **negrito**, blocos ```
  ...
example: |
  ...
summary: ...
activities:         # questões de recuperação, do mesmo tópico e publicadas
  - q.bd.norm.01
```

Texto em Markdown simples: linhas seguidas formam um parágrafo; separe parágrafos com uma
linha em branco. HTML nunca é interpretado.

## Validação

Um único erro rejeita a importação inteira. São verificados: campos obrigatórios, ids únicos,
referências (edital, tópico, questões das atividades), 5 alternativas no formato FGV, gabarito
existente, comentário em toda alternativa, alternativas com textos distintos, lições publicadas
só com questões publicadas.

Para validar localmente: `cd apps/api && ./mvnw test -Dtest=ContentValidatorTest`.

## Versionamento

- Cada importação registra a impressão digital do conteúdo (`content_import`).
- Alterar uma questão gera nova `revision` e um registro em `question_revision`; tentativas
  antigas continuam associadas à revisão respondida.
- Remover um item dos arquivos o marca como `RETIRED`, sem apagar histórico.
