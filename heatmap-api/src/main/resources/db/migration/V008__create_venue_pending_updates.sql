CREATE TABLE venue_pending_updates (
  venue_id       uuid             PRIMARY KEY REFERENCES venues (id) ON DELETE CASCADE,
  name           varchar(255)     NOT NULL,
  address        varchar(255)     NOT NULL,
  lat            double precision NOT NULL,
  lng            double precision NOT NULL,
  h3_res9        bigint           NOT NULL,
  category       varchar(64)      NOT NULL CHECK (category IN ('FOOD', 'ENTERTAINMENT', 'SHOPPING')),
  description    text,
  dish_of_day    varchar(255),
  music          varchar(255),
  status         varchar(32)      NOT NULL CHECK (status IN ('PENDING_UPDATE', 'REJECTED')),
  reject_reason  text,
  created_at     timestamptz      NOT NULL DEFAULT now(),
  updated_at     timestamptz      NOT NULL DEFAULT now()
);

CREATE INDEX idx_venue_pending_updates_status ON venue_pending_updates (status);
