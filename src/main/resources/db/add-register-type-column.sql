ALTER TABLE admins ADD COLUMN IF NOT EXISTS register_type VARCHAR(20);
UPDATE admins SET register_type = 'EMAIL' WHERE register_type IS NULL;
ALTER TABLE admins ALTER COLUMN register_type SET NOT NULL;

ALTER TABLE baristas ADD COLUMN IF NOT EXISTS register_type VARCHAR(20);
UPDATE baristas SET register_type = 'EMAIL' WHERE register_type IS NULL;
ALTER TABLE baristas ALTER COLUMN register_type SET NOT NULL;

ALTER TABLE customers ADD COLUMN IF NOT EXISTS register_type VARCHAR(20);
UPDATE customers SET register_type = 'EMAIL' WHERE register_type IS NULL;
ALTER TABLE customers ALTER COLUMN register_type SET NOT NULL;