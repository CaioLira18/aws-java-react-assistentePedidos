# 0005 - CloudFront com duas origens

Status: aceito (item de TLS na origem em aberto)

## Contexto
O front estático e a API precisam de HTTPS e de um único domínio, sem CORS.

## Decisão
Uma distribuição CloudFront: S3 como origem padrão e a EC2 como origem de `/api/*`, sem cache e com tratamento adequado para streaming. A EC2 aceita tráfego apenas da prefix list do CloudFront e valida um header secreto.

## Em aberto
HTTPS entre CloudFront e EC2. Origem em HTTP trafega em claro pela internet. O correto é um domínio próprio com proxy (Caddy) e certificado. Decidir antes de divulgar o link.

## Consequências
Domínio único, sem CORS e com a API protegida de acesso direto.
