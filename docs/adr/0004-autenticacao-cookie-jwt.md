# ADR 0004 — Sessão em cookie httpOnly com JWT, API na mesma origem

**Status:** aceito · 2026-10-21

## Contexto
A API precisa ser stateless e proteger os dados do estudante (LGPD). Guardar tokens em
`localStorage` os expõe a qualquer XSS.

## Decisão
Login e cadastro emitem um JWT (HS256, validade configurável) em cookie `httpOnly`,
`SameSite=Strict` e `Secure` em produção. O frontend chama a API pela própria origem (rewrite
`/api/*` no Next.js), eliminando CORS. A API também aceita `Authorization: Bearer` para clientes
não navegador. Senhas com BCrypt; login com tempo constante para e-mails inexistentes.

## Consequências
- O token não é acessível por JavaScript; `SameSite=Strict` mitiga CSRF na API JSON.
- Não há revogação individual de tokens antes da expiração; se necessário, adicionar lista de
  revogação ou tokens de atualização.
- O segredo `FOCAQUEST_JWT_SECRET` deve ser definido em todo ambiente compartilhado.
