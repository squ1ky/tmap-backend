CREATE TABLE cluster_anomalies
(
    h3_index     BIGINT         NOT NULL,
    resolution   SMALLINT       NOT NULL,
    hour_bucket  TIMESTAMPTZ    NOT NULL,
    tx_count     INTEGER        NOT NULL,
    baseline_avg NUMERIC(12, 2) NOT NULL,
    ratio        NUMERIC(8, 2)  NOT NULL,
    computed_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT pk_cluster_anomalies
        PRIMARY KEY (h3_index, resolution, hour_bucket)
);

CREATE INDEX ix_cluster_anomalies_resolution_bucket
    ON cluster_anomalies (resolution, hour_bucket DESC);