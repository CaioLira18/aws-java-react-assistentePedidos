CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE dish (
  id            BIGSERIAL PRIMARY KEY,
  name          TEXT NOT NULL,
  description   TEXT NOT NULL,
  price_cents   INTEGER NOT NULL CHECK (price_cents > 0),
  prep_minutes  INTEGER NOT NULL CHECK (prep_minutes > 0),
  dietary_tags  TEXT[] NOT NULL DEFAULT '{}',
  embedding     vector(1024),
  active        BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX dish_embedding_idx ON dish USING hnsw (embedding vector_cosine_ops);
CREATE INDEX dish_dietary_tags_idx ON dish USING gin (dietary_tags);

CREATE TABLE cart (
  id          UUID PRIMARY KEY,
  session_id  TEXT NOT NULL UNIQUE,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE cart_item (
  cart_id   UUID NOT NULL REFERENCES cart(id) ON DELETE CASCADE,
  dish_id   BIGINT NOT NULL REFERENCES dish(id),
  quantity  INTEGER NOT NULL CHECK (quantity > 0),
  PRIMARY KEY (cart_id, dish_id)
);

CREATE TABLE chat_message (
  id          BIGSERIAL PRIMARY KEY,
  session_id  TEXT NOT NULL,
  role        TEXT NOT NULL CHECK (role IN ('user','assistant','tool')),
  content     TEXT NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX chat_message_session_idx ON chat_message (session_id, id);

CREATE TABLE llm_call (
  id            BIGSERIAL PRIMARY KEY,
  session_id    TEXT NOT NULL,
  model         TEXT NOT NULL,
  input_tokens  INTEGER,
  output_tokens INTEGER,
  latency_ms    INTEGER,
  cost_usd      NUMERIC(10,6),
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
