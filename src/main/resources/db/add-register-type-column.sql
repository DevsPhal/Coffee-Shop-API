-- Adds User.registerType (EMAIL vs TELEGRAM — see RegisterType.java) to every concrete User
-- table. With no migration tool, ddl-auto: update cannot safely add a NOT NULL column to a table
-- that already has rows (no default to backfill with) — see add-product-sell-unit-columns.sql for
-- the same constraint. This script does it by hand, then ddl-auto: update just verifies on next
-- boot.
--
-- Safe to run more than once. Run against every database that already has these tables: local
-- dev DB and production. Run this AFTER deploying the code that adds User.registerType.
--
-- Every existing row predates Telegram registration/invites, so all of them backfill to 'EMAIL'.
ALTER TABLE admins ADD COLUMN IF NOT EXISTS register_type VARCHAR(20);
UPDATE admins SET register_type = 'EMAIL' WHERE register_type IS NULL;
ALTER TABLE admins ALTER COLUMN register_type SET NOT NULL;

ALTER TABLE baristas ADD COLUMN IF NOT EXISTS register_type VARCHAR(20);
UPDATE baristas SET register_type = 'EMAIL' WHERE register_type IS NULL;
ALTER TABLE baristas ALTER COLUMN register_type SET NOT NULL;

ALTER TABLE customers ADD COLUMN IF NOT EXISTS register_type VARCHAR(20);
UPDATE customers SET register_type = 'EMAIL' WHERE register_type IS NULL;
ALTER TABLE customers ALTER COLUMN register_type SET NOT NULL;
