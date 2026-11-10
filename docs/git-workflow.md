# Fluxo de trabalho com Git

Convenções de commits, branches e Pull Requests deste projeto. O objetivo é manter o histórico limpo, legível e fácil de auditar.

Idioma: commits, branches e títulos de PR em inglês. Documentação em português.

## Commits (Conventional Commits)

Formato:

```
<tipo>(<escopo>): <descrição curta>
```

### Tipos

| Tipo | Quando usar |
|---|---|
| `feat` | funcionalidade nova |
| `fix` | correção de bug |
| `refactor` | muda a estrutura do código sem mudar o comportamento |
| `perf` | melhora de desempenho |
| `test` | testes novos ou ajustados |
| `docs` | README, ADRs e demais documentos |
| `build` | dependências, Dockerfile, `pom.xml`, `package.json` |
| `ci` | GitHub Actions |
| `chore` | manutenção que não cabe nos outros (`.gitignore`, configs) |
| `style` | formatação, sem mudança de lógica |
| `revert` | desfaz um commit anterior |

Não existe o tipo `update`. Escolha o tipo que descreve a mudança: `feat` se adiciona algo, `fix` se corrige, `refactor` se reorganiza, `perf` se acelera.

### Escopos

Acompanham os módulos do projeto: `catalog`, `ordering`, `assistant`, `eval`, `db`, `api`, `web`, `shared`, `infra`, `deps`.

### Regras

- Verbo no imperativo e em minúsculas, sem ponto final (`add`, não `added`).
- Primeira linha com até 72 caracteres.
- Um commit por mudança lógica.
- O corpo do commit (opcional) explica o porquê, não o quê.
- Referencie issues no rodapé: `Closes #12`.
- Mudança que quebra compatibilidade usa `!` depois do tipo e `BREAKING CHANGE:` no rodapé.

### Exemplos

```
chore: add project skeleton and docker compose
docs: add initial readme
feat(db): add initial schema with pgvector
feat(catalog): add fictional dish seed generator
feat(catalog): add hybrid search with sql filters and vector ranking
feat(assistant): add LlmClient port and fake implementation
feat(assistant): add bedrock adapter using converse api
fix(assistant): stop agent loop after max tool iterations
perf(api): cache dashboard queries
refactor(catalog): extract search filters into value object
test(catalog): add testcontainers integration tests for search
ci: add backend build and test workflow
build(deps): add aws sdk bedrockruntime
docs(adr): record decision to use ec2 instead of ecs
```

Exemplo com corpo e rodapé:

```
fix(assistant): stop agent loop after max tool iterations

The model could keep requesting tools indefinitely, which inflated
latency and cost. The orchestrator now stops after 5 iterations and
returns a fallback message.

Closes #14
```

## Branches

Desenvolvimento baseado em `main`, com branches curtas. GitFlow seria exagero para um projeto individual.

- `main` está sempre estável e implantável.
- Toda mudança nasce numa branch de vida curta (1 a 3 dias).
- Nome: `<tipo>/<descrição-em-kebab-case>`, usando os mesmos tipos dos commits.
- Toda mudança entra por Pull Request, com squash merge. O título do PR já segue o padrão de commit.
- Depois do primeiro workflow de CI, ativar a proteção da `main`: exigir PR e checks verdes.
- Versões marcadas com tag (`v0.1.0` no primeiro deploy funcional).

Exceção: os dois primeiros commits (skeleton e README inicial) podem ir direto na `main`.

### Previsão de branches

Esta lista é um plano e pode mudar ao longo do projeto.

**Semana 1: base e busca**

- `chore/backend-bootstrap`: projeto Spring Initializr, configuração, Actuator (FEITO)
- `feat/db-schema`: migração Flyway e conexão com o banco (FEITO)
- `feat/catalog-seed-generator`: gerador do catálogo fictício (FEITO)
- `feat/embedding-client`: geração de embeddings pelo Bedrock (FEITO)
- `feat/catalog-hybrid-search`: filtros SQL + pgvector, com testes de integração (FEITO)
- `ci/backend-pipeline`: build e testes no GitHub Actions (FEITO)

**Semana 2: agente e chat**

- `feat/llm-client-port`: porta `LlmClient`, `FakeLlmClient` e adaptador Bedrock (FEITO)
- `feat/agent-orchestrator`: loop do agente com limite de iterações (FEITO)
- `feat/agent-search-tools`: tools `search_dishes` e `get_dish` (FEITO)
- `feat/cart-api`: módulo `ordering` e carrinho (FEITO)
- `feat/agent-cart-tools`: tools `add_to_cart` e `view_cart` (FEITO)
- `feat/chat-streaming-api`: endpoint SSE (FEITO)
- `feat/llm-call-metrics`: persistência de tokens, latência e custo (FEITO)
- `chore/frontend-bootstrap`: Vite + React + TypeScript (FEITO)
- `feat/chat-ui`: tela de chat com streaming (FEITO)
- `feat/dish-cards-and-cart-ui`: cards de pratos e carrinho

**Semana 3: deploy e prova**

- `feat/rate-limiting`: limites de uso antes de expor o link
- `ci/frontend-pipeline`: build e testes do front
- `chore/production-compose`: Dockerfile e compose de produção
- `ci/deploy-workflow`: deploy automático na EC2
- `docs/deploy-runbook`: passo a passo de CloudFront, S3 e EC2
- `feat/evaluation-runner`: execução dos 30 pedidos de teste
- `docs/evaluation-results`: números reais no README

**Ao longo do projeto**

- Bugs ganham branches `fix/...` (por exemplo `fix/sse-through-cloudfront`).

**Fase 2**

- `feat/checkout`
- `feat/auth-cognito`

## Pull Requests

- Título no padrão de commit: `feat(catalog): add hybrid search`.
- Descrição curta com três pontos: o que mudou, por que mudou e como testar.
- Um PR por branch, pequeno o bastante para ser lido de uma vez.
- Só fazer merge com o CI verde.
- Usar `Closes #N` para ligar o PR à issue do roadmap.

Modelo de descrição:

```markdown
## O que mudou
-

## Por que
-

## Como testar
-
```

## Fluxo do dia a dia

```bash
git switch main && git pull
git switch -c feat/db-schema

# trabalhar e commitar
git add .
git commit -m "feat(db): add initial schema with pgvector"

git push -u origin feat/db-schema
# abrir o PR no GitHub, esperar o CI e fazer squash merge

git switch main && git pull
git branch -d feat/db-schema
```

## Issues

Cada item do roadmap do README vira uma issue. Branch, PR e issue ficam ligados por `Closes #N`, o que mostra a rastreabilidade do trabalho do pedido até a entrega.
