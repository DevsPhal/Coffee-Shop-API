-- Stock moves from stock units (cartons) to base units (the sell unit, e.g. cans).
-- Stop the API, run this once, then start the version that counts stock in base units.
-- It multiplies quantities, so it records itself and refuses to run a second time.
BEGIN;

CREATE TABLE IF NOT EXISTS applied_data_migrations (
    name VARCHAR(100) PRIMARY KEY,
    applied_at TIMESTAMP NOT NULL DEFAULT now()
);

DO $$
DECLARE
    fractional TEXT;
BEGIN
    IF EXISTS (SELECT 1 FROM applied_data_migrations WHERE name = 'convert-inventory-to-base-units') THEN
        RAISE EXCEPTION 'convert-inventory-to-base-units was already applied; running it again would multiply stock again';
    END IF;
    SELECT string_agg(sku || ' (' || units_per_stock || ')', ', ') INTO fractional
    FROM products
    WHERE units_per_stock < 1 OR units_per_stock <> trunc(units_per_stock);
    IF fractional IS NOT NULL THEN
        RAISE EXCEPTION 'Units per stock must be a whole number of at least 1 first. Fix: %', fractional;
    END IF;
END $$;

ALTER TABLE product_size_options ADD COLUMN IF NOT EXISTS units_per_sale INTEGER;
UPDATE product_size_options SET units_per_sale = 1 WHERE units_per_sale IS NULL;
ALTER TABLE product_size_options ALTER COLUMN units_per_sale SET DEFAULT 1;
ALTER TABLE product_size_options ALTER COLUMN units_per_sale SET NOT NULL;

-- Every expense so far was entered in the product's stock unit.
ALTER TABLE stock_expenses ADD COLUMN IF NOT EXISTS unit VARCHAR(20);
UPDATE stock_expenses e SET unit = p.stock_unit FROM products p WHERE p.id = e.product_id AND e.unit IS NULL;

-- Batch cost becomes the cost of one can, which needs more than cents.
ALTER TABLE stock_batches ALTER COLUMN unit_cost TYPE NUMERIC(14,6);

-- Sales used to cut 0.042 of a carton per can; rounding to the nearest can drops that drift.
UPDATE inventories i
SET quantity_on_hand = ROUND(i.quantity_on_hand * p.units_per_stock),
    reorder_level = ROUND(i.reorder_level * p.units_per_stock)
FROM products p
WHERE p.id = i.product_id;

-- Rounded on the running total, so a product's batches still add up to its quantity on hand.
WITH running AS (
    SELECT b.id,
           ROUND(SUM(b.remaining_quantity) OVER w * p.units_per_stock)
               - ROUND((SUM(b.remaining_quantity) OVER w - b.remaining_quantity) * p.units_per_stock)
               AS remaining_base
    FROM stock_batches b
    JOIN products p ON p.id = b.product_id
    WINDOW w AS (PARTITION BY b.product_id ORDER BY b.created_at, b.id
                 ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)
)
UPDATE stock_batches b
SET remaining_quantity = r.remaining_base,
    quantity = ROUND(b.quantity * p.units_per_stock),
    unit_cost = b.unit_cost / p.units_per_stock
FROM running r, products p
WHERE r.id = b.id AND p.id = b.product_id;

UPDATE stock_movements m
SET quantity = ROUND(m.quantity * p.units_per_stock)
FROM products p
WHERE p.id = m.product_id;

-- Movements now say why stock moved, which order it was for, what it was worth and what was left after.
ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS reason VARCHAR(20);
ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS waste_reason VARCHAR(20);
ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS order_id UUID;
ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS total_cost NUMERIC(14,4);
ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS balance_after NUMERIC(12,3);
UPDATE stock_movements SET reason = 'PURCHASE' WHERE reason IS NULL AND type = 'STOCK_IN';
UPDATE stock_movements
SET reason = 'SALE',
    order_id = CAST(substring(note FROM 'Sold in order ([0-9a-f-]{36})') AS UUID)
WHERE reason IS NULL AND type = 'STOCK_OUT' AND note ~ '^Sold in order [0-9a-f-]{36}';
UPDATE stock_movements SET reason = 'ADJUSTMENT' WHERE reason IS NULL;
UPDATE stock_movements m SET total_cost = e.amount
FROM stock_expenses e
WHERE e.stock_movement_id = m.id AND m.total_cost IS NULL;
CREATE INDEX IF NOT EXISTS idx_stock_movements_order_reason ON stock_movements (order_id, reason);
CREATE INDEX IF NOT EXISTS idx_stock_movements_created_at ON stock_movements (created_at);

-- Stock that no longer matches its batches was already out of step before; it needs a count.
DO $$
DECLARE
    mismatched TEXT;
BEGIN
    SELECT string_agg(p.sku || ' (on hand ' || i.quantity_on_hand || ', batches ' || COALESCE(b.total, 0) || ')', ', ')
    INTO mismatched
    FROM inventories i
    JOIN products p ON p.id = i.product_id
    LEFT JOIN (SELECT product_id, SUM(remaining_quantity) AS total FROM stock_batches GROUP BY product_id) b
        ON b.product_id = i.product_id
    WHERE i.quantity_on_hand <> COALESCE(b.total, 0);
    IF mismatched IS NOT NULL THEN
        RAISE NOTICE 'Count these products and correct them with a stock-in or stock-cut: %', mismatched;
    END IF;
END $$;

INSERT INTO applied_data_migrations (name) VALUES ('convert-inventory-to-base-units');

COMMIT;
