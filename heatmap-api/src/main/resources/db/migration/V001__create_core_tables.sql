CREATE TABLE users (
  id            uuid         PRIMARY KEY,
  email         varchar(255) UNIQUE NOT NULL,
  password_hash varchar(255) NOT NULL,
  nickname      varchar(255) NOT NULL,
  role          varchar(32)  NOT NULL,
  blocked       boolean      NOT NULL DEFAULT false,
  created_at    timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE refresh_tokens (
  id         uuid         PRIMARY KEY,
  user_id    uuid         NOT NULL REFERENCES users (id),
  token_hash varchar(255) NOT NULL,
  revoked    boolean      NOT NULL DEFAULT false,
  expires_at timestamptz  NOT NULL,
  created_at timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE venues (
  id            uuid             PRIMARY KEY,
  owner_id      uuid             NOT NULL REFERENCES users (id),
  name          varchar(255)     NOT NULL,
  address       varchar(255)     NOT NULL,
  lat           double precision NOT NULL,
  lng           double precision NOT NULL,
  h3_res9       bigint           NOT NULL,
  category      varchar(64)      NOT NULL,
  description   text,
  photo_url     varchar(255),
  dish_of_day   varchar(255),
  music         varchar(255),
  status        varchar(32)      NOT NULL DEFAULT 'PENDING',
  reject_reason text,
  created_at    timestamptz      NOT NULL DEFAULT now(),
  updated_at    timestamptz      NOT NULL DEFAULT now()
);

CREATE TABLE venue_promos (
  id          uuid         PRIMARY KEY,
  venue_id    uuid         NOT NULL REFERENCES venues (id) ON DELETE CASCADE,
  title       varchar(255) NOT NULL,
  description text,
  starts_at   timestamptz,
  ends_at     timestamptz,
  created_at  timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE transactions (
  id          uuid             PRIMARY KEY,
  venue_id    uuid             NOT NULL REFERENCES venues (id),
  amount      decimal(12, 2)   NOT NULL,
  lat         double precision NOT NULL,
  lng         double precision NOT NULL,
  h3_res7     bigint           NOT NULL,
  h3_res8     bigint           NOT NULL,
  h3_res9     bigint           NOT NULL,
  category    varchar(64)      NOT NULL,
  occurred_at timestamptz      NOT NULL
);

CREATE TABLE cluster_history (
  h3_index   bigint         NOT NULL,
  resolution smallint       NOT NULL,
  category   varchar(64)    NOT NULL,
  hour_bucket timestamptz NOT NULL,
  tx_count   integer        NOT NULL,
  avg_check  decimal(12, 2) NOT NULL,
  sum_amount decimal(14, 2) NOT NULL,
  created_at timestamptz    NOT NULL DEFAULT now(),
  PRIMARY KEY (h3_index, resolution, category, hour_bucket)
);

CREATE TABLE loyalty_rules (
  id               uuid           PRIMARY KEY,
  venue_id         uuid           NOT NULL REFERENCES venues (id) ON DELETE CASCADE,
  description      varchar(255)   NOT NULL,
  discount_percent decimal(5, 2) NOT NULL,
  max_usages       integer        NOT NULL,
  active           boolean        NOT NULL DEFAULT true,
  created_at       timestamptz    NOT NULL DEFAULT now()
);

CREATE TABLE loyalty_verifications (
  id                uuid           PRIMARY KEY,
  venue_id          uuid           NOT NULL REFERENCES venues (id),
  user_id           uuid           NOT NULL REFERENCES users (id),
  rule_id           uuid           NOT NULL REFERENCES loyalty_rules (id),
  discount_applied decimal(5, 2) NOT NULL,
  verified_at       timestamptz    NOT NULL DEFAULT now()
);

CREATE TABLE districts (
  id         uuid         PRIMARY KEY,
  name       varchar(255) NOT NULL,
  city       varchar(255) NOT NULL DEFAULT 'Казань',
  photo_url varchar(255),
  created_at timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE h3_to_district (
  h3_index    bigint   PRIMARY KEY,
  district_id uuid     NOT NULL REFERENCES districts (id),
  resolution smallint NOT NULL
);

CREATE INDEX idx_venues_h3_res9 ON venues (h3_res9);
CREATE INDEX idx_venues_status ON venues (status);
CREATE INDEX idx_transactions_h3_res9_occurred_at ON transactions (h3_res9, occurred_at);
CREATE INDEX idx_transactions_h3_res8_occurred_at ON transactions (h3_res8, occurred_at);
CREATE INDEX idx_transactions_h3_res7_occurred_at ON transactions (h3_res7, occurred_at);
CREATE INDEX idx_transactions_occurred_at ON transactions (occurred_at);
CREATE INDEX idx_transactions_venue_id_occurred_at ON transactions (venue_id, occurred_at);
CREATE INDEX idx_cluster_history_hour_bucket ON cluster_history (hour_bucket);
CREATE UNIQUE INDEX uq_loyalty_verifications_rule_user
  ON loyalty_verifications (rule_id, user_id);
CREATE UNIQUE INDEX uq_h3_to_district_h3_index ON h3_to_district (h3_index);
