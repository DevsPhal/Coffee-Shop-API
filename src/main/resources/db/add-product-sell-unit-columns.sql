-- Renames Product.unit to Product.stockUnit and adds Product.sellUnit / Product.unitsPerStock.
-- With no migration tool, ddl-auto: update cannot rename a column (it would just add a new one
-- and orphan "unit") and cannot safely add NOT NULL columns to a table that already has rows (it
-- has no default value to backfill with) — see fix-enum-check-constraints.sql for the same
-- constraint. This script does both by hand, then ddl-auto: update just verifies on next boot.
--
-- Safe to run more than once. Run against every database that already has a "products" table:
-- local dev DB and production.
--
-- sell_unit backfills to 'CUP' for every existing row — update it per-product afterwards (PLATE
-- for noodles, BOTTLE for water, CAN for beer, etc.) via the product update endpoint or directly:
--   UPDATE products SET sell_unit = 'PLATE' WHERE sku IN (...);
-- units_per_stock backfills to 1 (stock and sell units are the same by default) — adjust
-- per-product where the ratio actually varies.

ALTER TABLE products RENAME COLUMN unit TO stock_unit;

ALTER TABLE products ADD COLUMN IF NOT EXISTS sell_unit VARCHAR(20);
UPDATE products SET sell_unit = 'CUP' WHERE sell_unit IS NULL;
ALTER TABLE products ALTER COLUMN sell_unit SET NOT NULL;

ALTER TABLE products ADD COLUMN IF NOT EXISTS units_per_stock NUMERIC(12,3);
UPDATE products SET units_per_stock = 1 WHERE units_per_stock IS NULL;
ALTER TABLE products ALTER COLUMN units_per_stock SET NOT NULL;
