CREATE TABLE loyalty_qr_sessions (
  id           uuid         PRIMARY KEY,
  token_hash   varchar(255) NOT NULL UNIQUE,
  user_id      uuid         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  venue_id     uuid         NOT NULL REFERENCES venues (id) ON DELETE CASCADE,
  rule_id      uuid         NOT NULL REFERENCES loyalty_rules (id) ON DELETE CASCADE,
  expires_at   timestamptz  NOT NULL,
  consumed_at  timestamptz,
  created_at   timestamptz  NOT NULL DEFAULT now()
);
