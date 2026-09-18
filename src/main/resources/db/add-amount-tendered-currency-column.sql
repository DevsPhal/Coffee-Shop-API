ALTER TABLE orders ADD COLUMN IF NOT EXISTS amount_tendered_currency VARCHAR(3);
UPDATE orders SET amount_tendered_currency = 'USD'
WHERE amount_tendered IS NOT NULL AND amount_tendered_currency IS NULL;