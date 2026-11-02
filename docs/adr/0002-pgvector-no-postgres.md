# 0002 - Busca vetorial com pgvector

Status: aceito

## Contexto
Precisamos de busca por significado combinada com filtros (preço, restrição alimentar, tempo de preparo) sobre poucas centenas de pratos.

## Decisão
Usar a extensão pgvector no próprio PostgreSQL, com índice HNSW e filtros SQL na mesma consulta.

## Alternativas
OpenSearch ou banco vetorial dedicado: mais um serviço para operar e pagar, desnecessário nesta escala.

## Consequências
Um banco só e consultas híbridas simples. A dimensão do vetor fica atrelada ao modelo de embeddings; trocar de modelo exige recalcular os embeddings.
