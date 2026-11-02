# 0001 - Monólito modular

Status: aceito

## Contexto
Projeto de uma pessoa, domínio pequeno, um único deploy.

## Decisão
Uma aplicação Spring Boot dividida em módulos por funcionalidade (`catalog`, `ordering`, `assistant`).

## Alternativas
Microsserviços: mais infraestrutura e complexidade sem ganho real nesta escala.

## Consequências
Deploy simples e refatoração fácil. Os limites entre módulos dependem de disciplina (regra: `domain` não depende de framework).
