# Deploy no Render (gratuito)

O arquivo [`render.yaml`](../render.yaml) descreve tudo: um PostgreSQL, a API e o frontend.

## Passo a passo

1. Crie uma conta em https://render.com (pode entrar com o GitHub).
2. No painel: **New → Blueprint**.
3. Conecte o GitHub e selecione o repositório `focaquest`. Se ele não aparecer, clique em
   *Configure account* e libere o acesso do Render ao repositório.
4. O Render lê o `render.yaml` e mostra os 3 recursos: `focaquest-db`, `focaquest-api` e
   `focaquest-web`. Clique em **Apply** (ou **Deploy Blueprint**).
5. Aguarde o primeiro build (10–15 min; a API compila o Java dentro do Docker).
6. Abra o serviço **focaquest-web** e clique no endereço `https://focaquest-web….onrender.com`.

## Se o endereço da API ficar diferente

O frontend procura a API em `https://focaquest-api.onrender.com`. Se o Render der outro nome ao
serviço (por exemplo `focaquest-api-x1y2.onrender.com`), atualize a variável:

**focaquest-web → Environment → `FOCAQUEST_API_URL`** = endereço exibido no topo do serviço
`focaquest-api`, e salve (o Render reinicia o serviço sozinho; não precisa rebuild).

## Limites do plano gratuito

- Os serviços **dormem após ~15 min sem acesso**. O primeiro acesso depois disso leva de 1 a
  2 minutos (frontend e API acordando). Enquanto a API acorda, o app mostra
  "O servidor está iniciando…"; é só tentar de novo.
- O PostgreSQL gratuito do Render **expira após um período** (atualmente 30 dias) e os dados são
  apagados; o painel avisa antes. Para manter os dados, mude o banco para um plano pago ou
  aponte `FOCAQUEST_DB_*` para outro PostgreSQL.
- 512 MB de memória por serviço: a API já vem configurada para caber nisso.

## Atualizações

Todo push na branch `main` dispara um novo deploy automático dos serviços.
O conteúdo em `content/` é reimportado na inicialização da API (de forma idempotente).
