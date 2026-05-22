TRUNCATE TABLE cluster_history;

ALTER TABLE cluster_history DROP CONSTRAINT cluster_history_pkey;
ALTER TABLE cluster_history DROP COLUMN category;
ALTER TABLE cluster_history ADD PRIMARY KEY (h3_index, resolution, hour_bucket);
ALTER TABLE cluster_history ADD COLUMN h3_parent_res6 BIGINT NOT NULL;

CREATE INDEX idx_cluster_history_parent_res_hour
    ON cluster_history (h3_parent_res6, resolution, hour_bucket);