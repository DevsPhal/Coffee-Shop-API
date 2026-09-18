ALTER TABLE products RENAME COLUMN unit TO stock_unit;

ALTER TABLE products ADD COLUMN IF NOT EXISTS sell_unit VARCHAR(20);
UPDATE products SET sell_unit = 'CUP' WHERE sell_unit IS NULL;
ALTER TABLE products ALTER COLUMN sell_unit SET NOT NULL;

ALTER TABLE products ADD COLUMN IF NOT EXISTS units_per_stock NUMERIC(12,3);
UPDATE products SET units_per_stock = 1 WHERE units_per_stock IS NULL;
ALTER TABLE products ALTER COLUMN units_per_stock SET NOT NULL;