# 0003 - EC2 com Docker Compose em vez de ECS + RDS

Status: aceito

## Contexto
ALB e RDS cobram por hora mesmo parados, e a configuração de ECS, rede e IaC consome muitas horas. O objetivo é ter o projeto no ar com custo baixo.

## Decisão
Uma EC2 pequena (2 GB de RAM) rodando Spring Boot e PostgreSQL via Docker Compose. Front-end em S3 + CloudFront. IA no Bedrock.

## Alternativas
ECS Fargate + RDS + ALB: mais robusto, mais caro e mais lento de montar.

## Consequências
Ponto único de falha e manutenção manual (aceitável para portfólio). Backup via pg_dump para o S3. Migração para ECS + RDS fica como fase 3.
