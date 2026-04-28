ALTER TABLE loyalty_rules
    ALTER COLUMN discount_percent TYPE integer
    USING ROUND(discount_percent)::integer;

ALTER TABLE loyalty_verifications
    ALTER COLUMN discount_applied TYPE integer
    USING ROUND(discount_applied)::integer;
