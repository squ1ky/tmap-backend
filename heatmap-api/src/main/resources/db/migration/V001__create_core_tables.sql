CREATE TABLE users (
  id uuid PRIMARY KEY,
  email varchar(255) UNIQUE NOT NULL,
  password_hash varchar(255) NOT NULL,
  nickname varchar(255) NOT NULL,
  role varchar(32) NOT NULL,
  blocked boolean NOT NULL DEFAULT false,
  created_at timestamp NOT NULL DEFAULT now()
);

CREATE TABLE refresh_tokens (
  id uuid PRIMARY KEY,
  user_id uuid NOT NULL,
  token_hash varchar(255) NOT NULL,
  revoked boolean NOT NULL DEFAULT false,
  expires_at timestamp NOT NULL,
  created_at timestamp NOT NULL DEFAULT now()
);

CREATE TABLE venues (
  id uuid PRIMARY KEY,
  owner_id uuid NOT NULL,
  name varchar(255) NOT NULL,
  address varchar(255) NOT NULL,
  lat double precision NOT NULL,
  lng double precision NOT NULL,
  h3_res9 bigint NOT NULL,
  category varchar(64) NOT NULL,
  description text,
  photo_url varchar(255),
  dish_of_day varchar(255),
  music varchar(255),
  status varchar(32) NOT NULL DEFAULT 'PENDING',
  reject_reason text,
  created_at timestamp NOT NULL DEFAULT now(),
  updated_at timestamp NOT NULL DEFAULT now()
);

CREATE TABLE venue_promos (
  id uuid PRIMARY KEY,
  venue_id uuid NOT NULL,
  title varchar(255) NOT NULL,
  description text,
  starts_at timestamp,
  ends_at timestamp,
  created_at timestamp NOT NULL DEFAULT now()
);

CREATE TABLE transactions (
  id uuid PRIMARY KEY,
  venue_id uuid NOT NULL,
  amount decimal(12, 2) NOT NULL,
  lat double precision NOT NULL,
  lng double precision NOT NULL,
  h3_res7 bigint NOT NULL,
  h3_res8 bigint NOT NULL,
  h3_res9 bigint NOT NULL,
  category varchar(64) NOT NULL,
  occurred_at timestamp NOT NULL
);

CREATE TABLE cluster_history (
  h3_index bigint NOT NULL,
  resolution smallint NOT NULL,
  category varchar(64) NOT NULL,
  hour_bucket timestamp NOT NULL,
  tx_count integer NOT NULL,
  avg_check decimal(12, 2) NOT NULL,
  sum_amount decimal(14, 2) NOT NULL,
  created_at timestamp NOT NULL DEFAULT now(),
  PRIMARY KEY (h3_index, resolution, category, hour_bucket)
);

CREATE TABLE loyalty_rules (
  id uuid PRIMARY KEY,
  venue_id uuid NOT NULL,
  description varchar(255) NOT NULL,
  discount_percent decimal(5, 2) NOT NULL,
  max_usages integer NOT NULL,
  active boolean NOT NULL DEFAULT true,
  created_at timestamp NOT NULL DEFAULT now()
);

CREATE TABLE loyalty_verifications (
  id uuid PRIMARY KEY,
  venue_id uuid NOT NULL,
  user_id uuid NOT NULL,
  rule_id uuid NOT NULL,
  discount_applied decimal(5, 2) NOT NULL,
  verified_at timestamp NOT NULL DEFAULT now()
);

CREATE TABLE districts (
  id uuid PRIMARY KEY,
  name varchar(255) NOT NULL,
  city varchar(255) NOT NULL DEFAULT 'Казань',
  photo_url varchar(255),
  created_at timestamp NOT NULL DEFAULT now()
);

CREATE TABLE h3_to_district (
  h3_index bigint PRIMARY KEY,
  district_id uuid NOT NULL,
  resolution smallint NOT NULL
);

ALTER TABLE refresh_tokens
  ADD CONSTRAINT fk_refresh_tokens_user_id
  FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE venues
  ADD CONSTRAINT fk_venues_owner_id
  FOREIGN KEY (owner_id) REFERENCES users (id);

ALTER TABLE venue_promos
  ADD CONSTRAINT fk_venue_promos_venue_id
  FOREIGN KEY (venue_id) REFERENCES venues (id);

ALTER TABLE transactions
  ADD CONSTRAINT fk_transactions_venue_id
  FOREIGN KEY (venue_id) REFERENCES venues (id);

ALTER TABLE loyalty_rules
  ADD CONSTRAINT fk_loyalty_rules_venue_id
  FOREIGN KEY (venue_id) REFERENCES venues (id);

ALTER TABLE loyalty_verifications
  ADD CONSTRAINT fk_loyalty_verifications_venue_id
  FOREIGN KEY (venue_id) REFERENCES venues (id);

ALTER TABLE loyalty_verifications
  ADD CONSTRAINT fk_loyalty_verifications_user_id
  FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE loyalty_verifications
  ADD CONSTRAINT fk_loyalty_verifications_rule_id
  FOREIGN KEY (rule_id) REFERENCES loyalty_rules (id);

ALTER TABLE h3_to_district
  ADD CONSTRAINT fk_h3_to_district_district_id
  FOREIGN KEY (district_id) REFERENCES districts (id);

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
