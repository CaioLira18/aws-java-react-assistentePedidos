# Mapa dos arquivos Java

Guia de para que serve cada arquivo `.java` do projeto **Assistente de Pedidos com IA**, no estado atual do desenvolvimento. Pacote base: `io.github.caiolira18.foodassistant`.

## Visão geral dos pacotes

| Pacote | Papel | Branch de origem |
|---|---|---|
| (raiz) | Ponto de entrada da aplicação Spring Boot | `chore/backend-bootstrap` |
| `catalog.seed` | Gera e insere o catálogo fictício de pratos | `feat/catalog-seed-generator` |
| `catalog.embedding` | Gera embeddings (vetores) dos pratos pelo Amazon Bedrock | `feat/embedding-client` |
| `catalog.search` | Busca híbrida (filtros SQL mais similaridade vetorial) e consulta de um prato por id | `feat/catalog-hybrid-search` e `feat/agent-search-tools` |
| `assistant.llm` | Porta (interface) do modelo de linguagem e seus tipos | `feat/llm-client-port` |
| `assistant.agent` | Orquestrador do agente: loop de ferramentas, limites e registro de tools | `feat/agent-orchestrator` |
| `assistant.agent.tools` | Ferramentas do agente: catálogo (`search_dishes`, `get_dish`) e carrinho (`add_to_cart`, `view_cart`) | `feat/agent-search-tools` e `feat/agent-cart-tools` |
| `assistant.metrics` | Registro de tokens, latência e custo de cada chamada ao modelo na tabela `llm_call` | `feat/llm-call-metrics` |
| `ordering` | Carrinho preso à sessão: regras, persistência e API REST | `feat/cart-api` |
| `chat` | Endpoint de chat em SSE, histórico da conversa e limites de uso | `feat/chat-streaming-api` |
| `shared` | Utilitários usados por mais de um módulo | `feat/chat-streaming-api` |

## Como as peças se conectam

```mermaid
flowchart LR
  subgraph catalogo["catalog"]
    SEED[DishSeedRunner] --> DB[(PostgreSQL + pgvector)]
    EMBRUN[DishEmbeddingRunner] --> EC[EmbeddingClient]
    EMBRUN --> DB
    EC --> BEC[BedrockEmbeddingClient]
    SS[DishSearchService] --> EC
    SS --> SR[DishSearchRepository]
    SR --> DB
  end
  subgraph pedido["ordering"]
    CCTL[CartController] --> CART[CartService]
    CART --> CREPO[CartRepository]
    CREPO --> DB
  end
  subgraph agente["assistant"]
    AO[AgentOrchestrator] --> LC[LlmClient]
    AO --> TR[ToolRegistry]
    TR --> AT[AgentTool]
    SDT[SearchDishesTool] -.->|implementa| AT
    GDT[GetDishTool] -.->|implementa| AT
    ADT[AddToCartTool] -.->|implementa| AT
    VCT[ViewCartTool] -.->|implementa| AT
    REC[DatabaseLlmCallRecorder] --> DB
  end
  subgraph conversa["chat"]
    CHC[ChatController] --> CHS[ChatService]
    CHS --> AO
    CHS --> CHR[ChatHistoryRepository]
    CHR --> DB
  end
  SDT --> SS
  GDT --> SS
  ADT --> CART
  VCT --> CART
```

Regras de projeto que aparecem em várias classes:

- **Portas e adaptadores:** `EmbeddingClient` e `LlmClient` isolam o provedor de IA. O domínio não conhece a AWS, e os testes usam *fakes* (ADR 0004).
- **Servidor é a fonte de verdade:** argumentos de busca e de ferramentas são validados no servidor, e o preço sempre vem do banco.
- **Ferramentas validam tudo:** cada tool recusa campos desconhecidos, tipos errados e valores fora da faixa antes de tocar o catálogo, e o erro volta ao modelo como JSON. As tools de catálogo só leem; `add_to_cart` altera o carrinho da sessão do `ToolContext`, nunca de uma sessão informada pelo modelo.
- **Chat com limites no servidor:** uma conversa por vez por sessão, no máximo 8 turnos simultâneos, mensagem de até 500 caracteres, e erros internos só no log.
- **Perfis do Spring** (`seed`, `embed`, `search`) mantêm as tarefas de linha de comando fora da execução normal.

---

## Raiz

| Arquivo | Para que serve |
|---|---|
| `FoodassistantApplication.java` | Ponto de entrada da aplicação Spring Boot (`main`). Sobe o contexto, o Flyway e o servidor web. |

## `catalog.seed`: catálogo fictício

| Arquivo | Para que serve |
|---|---|
| `DishSeed.java` | *Record* com os dados de um prato gerado: nome, descrição, preço em centavos, tempo de preparo e tags alimentares. |
| `DishSeedGenerator.java` | Java puro, sem Spring. Gera de forma determinística 150 pratos (30 pratos base × 5 variações: normal, Fit, Grande, Família e Express). Garante coerência das tags: `vegano` implica `vegetariano` e `sem-lactose`. |
| `DishSeedRunner.java` | Executa só com o perfil `seed`. Insere os pratos no banco em lote com `JdbcTemplate`. É idempotente: se a tabela `dish` já tem pratos, não faz nada. Os embeddings ficam nulos nessa etapa. |

## `catalog.embedding`: vetores dos pratos

| Arquivo | Para que serve |
|---|---|
| `EmbeddingClient.java` | Porta (interface) com `float[] embed(String texto)`. A busca e o gerador dependem dela, não do Bedrock. |
| `BedrockConfig.java` | Configuração Spring que cria o `BedrockRuntimeClient`. Lê região e perfil AWS das propriedades. Sem perfil informado, usa a cadeia padrão de credenciais (a IAM role na EC2). |
| `BedrockEmbeddingClient.java` | Adaptador da porta: chama o modelo Titan Text Embeddings V2 no Bedrock (`InvokeModel`), com 1024 dimensões normalizadas, e valida a dimensão da resposta. |
| `DishEmbeddingRunner.java` | Executa só com o perfil `embed`. Para cada prato sem vetor, monta o texto (nome, descrição e restrições em linguagem natural), gera o embedding e grava no banco. É idempotente e registra o progresso a cada 25 pratos. |
| `VectorLiteral.java` | Converte um `float[]` para o formato de texto do pgvector (por exemplo `[0.5,-1.0]`), usado nos parâmetros SQL com `::vector`. |

## `catalog.search`: busca híbrida

| Arquivo | Para que serve |
|---|---|
| `DishSearchFilters.java` | *Record* com os filtros opcionais: preço máximo, tempo máximo e restrições obrigatórias. Valida no construtor (valores positivos e apenas as tags `vegano`, `vegetariano`, `sem-gluten` e `sem-lactose`). `none()` devolve "sem filtros". |
| `DishSearchResult.java` | *Record* de um resultado da busca: id, nome, descrição, preço, tempo de preparo, tags e similaridade de cosseno. |
| `DishDetails.java` | *Record* com os dados de um prato consultado por id: id, nome, descrição, preço, tempo de preparo e tags (sem similaridade). |
| `DishSearchRepository.java` | Executa uma única SQL com os filtros montados dinamicamente e ordenação por distância de cosseno (`<=>`). Todos os valores vão como parâmetros do `PreparedStatement`. Ignora pratos inativos ou sem embedding. Também tem `findActiveById`, que busca um prato ativo pelo id. |
| `DishSearchService.java` | Entrada pública da busca. Valida a consulta (não vazia, no máximo 300 caracteres) e o limite (1 a 20), gera o embedding da consulta e delega ao repositório. É a base da tool `search_dishes`. O método `getDish(id)` alimenta a `get_dish` (id inválido devolve vazio). |
| `DishSearchRunner.java` | Executa só com o perfil `search`. Demonstração por linha de comando: lê `SEARCH_QUERY`, `SEARCH_MAX_PRICE_CENTS`, `SEARCH_MAX_PREP_MINUTES` e `SEARCH_TAGS`, e imprime os 5 melhores pratos. |

## `assistant.llm`: porta do modelo de linguagem

| Arquivo | Para que serve |
|---|---|
| `LlmClient.java` | Porta (interface) do modelo: `generate(request)` para resposta completa e `stream(request, onToken)` para entregar o texto aos poucos. O adaptador do Bedrock ainda será criado. |
| `LlmRequest.java` | O que se envia ao modelo: prompt de sistema, mensagens, definições de ferramentas e limite de tokens da resposta. |
| `LlmResponse.java` | O que o modelo devolve: texto, pedidos de ferramenta, uso de tokens e motivo da parada (`END_TURN`, `TOOL_USE` ou `MAX_TOKENS`). `wantsTools()` indica se há ferramentas a executar. |
| `Message.java` | Uma mensagem da conversa, com papel `USER`, `ASSISTANT` ou `TOOL`. Tem fábricas `user`, `assistant` e `toolResult`. |
| `ToolCall.java` | Um pedido de ferramenta feito pelo modelo: id da chamada, nome e argumentos em JSON (texto). |
| `ToolDefinition.java` | Descrição de uma ferramenta exposta ao modelo: nome, descrição e JSON Schema dos argumentos. |
| `Usage.java` | Tokens de entrada e de saída de uma chamada, base para custo e métricas. |

## `assistant.agent`: orquestrador

| Arquivo | Para que serve |
|---|---|
| `AgentOrchestrator.java` | Núcleo do agente. Monta a requisição, chama o `LlmClient`, executa as ferramentas pedidas e repete até o modelo responder em texto. Aplica limite de iterações e timeout com mensagem de contingência, limita as chamadas de ferramenta por turno (toda chamada recebe um resultado, mesmo recusada) e corta o histórico sem separar pares de chamada e resultado. Com `onToken`, usa `stream`. |
| `AgentSettings.java` | Limites do agente: iterações (5), timeout (30 s), tokens por resposta (1024), mensagens de histórico (20) e chamadas de ferramenta por turno (4). `defaults()` traz esses valores. |
| `AgentResult.java` | Resultado de um turno: resposta final, desfecho, número de iterações, tokens, tempo gasto e as mensagens novas para persistir. |
| `AgentOutcome.java` | Enum do desfecho: `COMPLETED`, `MAX_TOKENS`, `ITERATION_LIMIT` ou `TIMEOUT`. |
| `AgentPrompt.java` | Prompt de sistema fixo: responde em português, não inventa preços nem pratos, trata texto de prato como dado (não como instrução) e só altera o carrinho a pedido explícito. |
| `AgentTool.java` | Interface de uma ferramenta do agente: `definition()` para o modelo e `execute(argumentosJson, contexto)`. Implementações atuais: `SearchDishesTool`, `GetDishTool`, `AddToCartTool` e `ViewCartTool`. |
| `ToolRegistry.java` | Registro e despacho das ferramentas. Rejeita nomes duplicados e argumentos acima de 2000 caracteres, devolve erro em JSON para ferramenta desconhecida ou `ToolException` e, em falha inesperada, registra o erro no log e devolve uma mensagem genérica sem vazar detalhes internos. |
| `AgentConfig.java` | Configuração Spring que monta o bean `ToolRegistry` com todas as ferramentas anotadas com `@Component`, então uma tool nova entra no registro sozinha. O `AgentOrchestrator` é criado em `ChatConfig`. |
| `ToolContext.java` | Contexto passado a cada ferramenta (por enquanto o `sessionId`), para que o carrinho fique preso à sessão. |
| `ToolException.java` | Erro de uso de uma ferramenta, como argumento inválido. A mensagem volta ao modelo para ele corrigir a chamada. |

## `assistant.agent.tools`: ferramentas do catálogo e do carrinho

| Arquivo | Para que serve |
|---|---|
| `SearchDishesTool.java` | Tool `search_dishes`. Exige `query` (até 300 caracteres) e aceita `max_price_cents`, `max_prep_minutes`, `dietary_tags` e `limit` (padrão 5, máximo 10). Normaliza restrições escritas de forma solta ("Sem Glúten" vira `sem-gluten`) e recusa as desconhecidas. Chama o `DishSearchService` e devolve JSON com `count` e `dishes`, cada prato com `price_cents`, `price_brl` e similaridade arredondada. Sem resultados, inclui uma dica (`note`) para o modelo. |
| `GetDishTool.java` | Tool `get_dish`. Exige `dish_id` inteiro positivo e devolve os detalhes do prato ativo. Prato inexistente ou inativo vira erro para o modelo. |
| `ToolArguments.java` | Validador estrito dos argumentos: rejeita campos desconhecidos, tipos errados (número em texto, decimal onde se espera inteiro), valores fora da faixa e listas inválidas, com mensagens claras que voltam ao modelo. |
| `ToolJson.java` | Lê o JSON de entrada como objeto (qualquer outra coisa vira erro padronizado) e serializa a resposta das ferramentas. |
| `DishToolFormat.java` | Formata o prato para o modelo: preço já em reais (`price_brl`, como "R$ 31,90"), para o modelo não calcular preço, descrição truncada em 300 caracteres e similaridade com 3 casas decimais. |
| `AddToCartTool.java` | Tool `add_to_cart`. Exige `dish_id` e aceita `quantity` (1 a 10, padrão 1). A sessão vem sempre do `ToolContext`, então um `session_id` nos argumentos é recusado. Regras do carrinho violadas voltam ao modelo como erro de tool. Devolve o carrinho atualizado. |
| `ViewCartTool.java` | Tool `view_cart`. Não recebe argumentos (aceita texto em branco) e devolve o carrinho da sessão do contexto. |
| `CartToolFormat.java` | Serializa o carrinho para o modelo: itens com preço unitário e total da linha já em reais (`price_brl`), total de itens, total do carrinho e a marca `is_empty`. |

## `assistant.metrics`: métricas das chamadas ao modelo

| Arquivo | Para que serve |
|---|---|
| `LlmCallRecord.java` | Dados de uma chamada ao modelo a registrar, no formato da tabela `llm_call`: sessão, modelo, tokens de entrada e de saída, latência e custo. |
| `LlmCallRecorder.java` | Interface para registrar uma chamada ao modelo, o que mantém o resto do código independente de onde o registro é gravado. |
| `DatabaseLlmCallRecorder.java` | Implementação que grava o registro no banco, usando o `LlmCallRepository`. |
| `LlmCallRepository.java` | Acesso SQL à tabela `llm_call`. |
| `CostCalculator.java` | Calcula o custo de uma chamada a partir dos tokens usados. |
| `MetricsConfig.java` | Configuração Spring do módulo de métricas. |

## `ordering`: carrinho

| Arquivo | Para que serve |
|---|---|
| `CartLine.java` | *Record* de uma linha do carrinho: prato, nome, preço unitário, quantidade e total da linha. O preço vem sempre do banco. |
| `CartView.java` | *Record* do carrinho inteiro: linhas, total de itens e total em centavos. `empty()` devolve um carrinho vazio. |
| `CartRepository.java` | Acesso SQL ao carrinho: cria ou trava (`FOR UPDATE`) o carrinho da sessão, confere se o prato está ativo, lê e grava quantidades, remove itens e monta as linhas juntando com `dish` para pegar o preço atual. |
| `CartService.java` | Regras do carrinho preso à sessão: no máximo 10 unidades por item e 20 itens diferentes, só pratos ativos, entradas validadas. Operações: ver, adicionar (soma a quantidade), definir quantidade, remover e limpar. Ver o carrinho de uma sessão desconhecida não cria nada. Violações viram `IllegalArgumentException` com mensagem em português. |
| `CartController.java` | API REST em `/api/cart`, com a sessão no header `X-Session-Id`: `GET`, `POST /items`, `PUT /items/{dishId}`, `DELETE /items/{dishId}` e `DELETE`. Erros de validação viram `400` com `{"error": "..."}`. |

## `chat`: conversa em streaming

| Arquivo | Para que serve |
|---|---|
| `ChatController.java` | `POST /api/chat`, com a sessão em `X-Session-Id` e o corpo `{"message": "..."}`. Responde em SSE com os eventos `token` (`{"text": ...}`), `done` (desfecho e iterações) e `error` (mensagem genérica). Roda o turno em thread virtual, interrompe se o cliente desconecta e nunca expõe erro interno. Rejeições viram `400`, `429` ou `503` em JSON. |
| `ChatService.java` | Valida a mensagem (não vazia, até 500 caracteres) e reserva a sessão e uma vaga: uma conversa por vez por sessão (`429`) e no máximo 8 turnos simultâneos (`503`). Carrega o histórico, chama o `AgentOrchestrator` e grava o turno. |
| `ChatHistoryRepository.java` | Lê as últimas mensagens de `user` e `assistant` de uma sessão em ordem cronológica e grava um turno inteiro (usuário e resposta) em um único `INSERT` na tabela `chat_message`. Não guarda os resultados das ferramentas. |
| `ChatConfig.java` | Configuração Spring: cria o bean `AgentOrchestrator`, com o `LlmClient` se existir ou com o `UnavailableLlmClient` e um aviso no log, e o executor de threads virtuais do chat. |
| `ChatRejectedException.java` | Rejeição de um turno (sessão ocupada ou sem vagas), com o status HTTP correspondente. |
| `UnavailableLlmClient.java` | `LlmClient` provisório que falha em toda chamada, usado enquanto não existe um adaptador real. |

## `shared`

| Arquivo | Para que serve |
|---|---|
| `SessionIds.java` | Valida o id de sessão (8 a 64 caracteres: letras, números e hífen). Carrinho e chat usam a mesma regra, então aceitam exatamente os mesmos ids. |

---

## Testes

### Raiz de testes

| Arquivo | Para que serve |
|---|---|
| `TestPostgres.java` | Auxiliar que sobe uma vez o container `pgvector/pgvector:pg16` com Testcontainers e devolve a URL JDBC. Os testes de carrinho e de histórico criam um banco próprio dentro dele. |
| `FoodassistantApplicationTests.java` | Sobe o contexto Spring completo contra um container pgvector (`@ServiceConnection`, nunca contra o Neon), o que valida a migração V1 e a montagem dos beans. Confirma também que o Spring registra no `ToolRegistry` as tools do agente. |

### `catalog`

| Arquivo | Para que serve |
|---|---|
| `DishSeedGeneratorTest.java` | Invariantes do catálogo: 150 pratos, nomes únicos, preços e tempos positivos, regra do `vegano` e determinismo. |
| `FakeEmbeddingClient.java` | `EmbeddingClient` falso que gera um vetor determinístico a partir do texto, para testes sem Bedrock. |
| `DishEmbeddingRunnerTest.java` | Testa o texto enviado ao modelo de embedding, o formato do literal do pgvector e o determinismo do fake. |
| `DishSearchFiltersTest.java` | Validação dos filtros: tag desconhecida, valores não positivos e `none()`. |
| `DishSearchServiceTest.java` | Com Mockito: consulta vazia ou longa, limite inválido (sem tocar no banco), delegação ao repositório e busca de prato por id. |
| `DishSearchRepositoryTest.java` | Teste de integração com Postgres e pgvector reais em container. Cobre ranking por similaridade, filtros de preço, tempo e tags, combinação de filtros, limite e exclusão de pratos inativos ou sem embedding. Cobre também a busca de prato ativo por id. |

### `assistant`

| Arquivo | Para que serve |
|---|---|
| `FakeLlmClient.java` | `LlmClient` roteirizado: `enqueue` define as respostas, `received()` mostra as requisições recebidas, e `text` e `toolCall` criam respostas prontas. O `stream` emite o texto de uma vez. |
| `FakeLlmClientTest.java` | Garante que o fake devolve as respostas na ordem e falha quando o roteiro acaba. |
| `ToolRegistryTest.java` | Despacho de ferramentas, erros devolvidos ao modelo, ocultação de detalhes internos, limite de argumentos, escape do JSON de erro e nomes duplicados. |
| `AgentOrchestratorTest.java` | Cenários do loop, todos com `FakeLlmClient`: resposta direta, uma ferramenta, argumento inválido, ferramenta desconhecida, limite de iterações, timeout com relógio controlado, streaming, corte de histórico, limite de chamadas por turno e parada por `MAX_TOKENS`. |
| `SearchDishesToolTest.java` | Com o serviço simulado: argumentos repassados ao serviço, padrões, normalização de restrições, formato do resultado (preço em R$ e similaridade), descrição truncada, dica para resultado vazio e uma bateria de argumentos inválidos que não podem tocar no catálogo. |
| `GetDishToolTest.java` | Detalhes do prato, prato inexistente e argumentos inválidos, sem tocar no catálogo. |
| `CatalogToolsFlowTest.java` | Fluxo completo com o orquestrador real, o `FakeLlmClient` e as duas tools: o agente busca e responde com dados da ferramenta, e um argumento inválido volta como erro e o agente se recupera. |
| `AddToCartToolTest.java` | Sessão sempre vinda do contexto, quantidade padrão 1, carrinho devolvido com preços em R$, `session_id` recusado, argumentos inválidos sem tocar no carrinho e violação de regra do carrinho como erro de tool. |
| `ViewCartToolTest.java` | Carrinho vazio, argumentos em branco aceitos, sessão do contexto e qualquer argumento recusado. |
| `CartToolsFlowTest.java` | Orquestrador real com `FakeLlmClient`: o agente adiciona ao carrinho da sessão atual e responde com o total. |
| `CostCalculatorTest.java` | Testa o cálculo de custo a partir dos tokens. |
| `DatabaseLlmCallRecorderTest.java` | Testa a gravação do registro de uma chamada ao modelo no banco. |

### `ordering`, `shared` e `chat`

| Arquivo | Para que serve |
|---|---|
| `CartServiceTest.java` | Integração com Postgres em container (banco `cart_test` próprio): preço vindo do banco, soma de quantidades, limites de 10 por item e 20 itens, pratos inativos ou inexistentes, entradas inválidas, definir, remover e limpar, isolamento por sessão e leitura sem criar carrinho. |
| `CartControllerTest.java` | Com MockMvc e serviço simulado: ver, adicionar, erro de validação como `400` com mensagem e header de sessão ausente. |
| `SessionIdsTest.java` | Aceita UUIDs e recusa nulo, curto, com acento e longo demais. |
| `ChatServiceTest.java` | Com Mockito: entradas inválidas, uma conversa por vez por sessão, limite de turnos simultâneos sem deixar sessão presa e o turno completo (histórico, agente e gravação). |
| `ChatControllerTest.java` | Com MockMvc e executor síncrono: stream de tokens (preservando espaços) e evento final, falha do agente como evento de erro genérico sem vazar detalhe e liberando a sessão, `400` para sessão ou mensagem inválida e `429` para sessão ocupada. |
| `ChatHistoryRepositoryTest.java` | Integração (banco `chat_test` próprio): ordem e papéis, limite das mensagens mais recentes e isolamento por sessão. |

---

## Arquivos não Java que esses códigos usam

- `db/migration/V1__initial_schema.sql`: tabelas `dish`, `cart`, `cart_item`, `chat_message` e `llm_call`.
- `application.properties` e `application-{seed,embed,search}.properties`: configuração e perfis.
- `.github/workflows/backend-ci.yml`: build e testes no GitHub Actions.

## Ainda não existe

Adaptador `BedrockLlmClient` (sem ele o chat responde com erro), limite de uso (rate limiting), front-end, deploy automático, avaliação com os 30 pedidos de teste e, na fase 2, checkout e autenticação com Cognito.