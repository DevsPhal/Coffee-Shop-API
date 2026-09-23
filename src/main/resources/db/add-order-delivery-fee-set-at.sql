-- Orders placed before delivery_fee_set_at existed: treat any delivery order that already has a
-- fee, or has moved past PENDING, as quoted so it isn't blocked waiting for a new quote.
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_fee_set_at timestamp;

UPDATE orders
SET delivery_fee_set_at = COALESCE(updated_at, created_at)
WHERE delivery_fee_set_at IS NULL
  AND (fulfillment_method = 'DELIVERY' OR delivery_latitude IS NOT NULL)
  AND (delivery_fee > 0 OR status <> 'PENDING' OR paid_at IS NOT NULL);
