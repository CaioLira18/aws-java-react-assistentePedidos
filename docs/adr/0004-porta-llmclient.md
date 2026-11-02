# 0004 - Porta LlmClient com adaptador Bedrock

Status: aceito

## Contexto
O agente depende de um modelo de linguagem, mas o domínio não deve conhecer a AWS, e os testes não podem chamar o modelo real.

## Decisão
Definir a interface `LlmClient` no módulo `assistant`. A implementação `BedrockLlmClient` usa o AWS SDK v2 (Converse API, com tool use e streaming). Os testes usam um `FakeLlmClient`.

## Alternativas
Spring AI: acelera, mas esconde detalhes que quero dominar.

## Consequências
Troca de modelo sem mexer na lógica do agente e testes determinísticos. Um pouco mais de código próprio.
