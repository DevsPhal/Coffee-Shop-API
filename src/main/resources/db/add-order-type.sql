-- POS (walk-in) orders get an order type, DINE_IN or TAKE_AWAY, in place of the old POS "Order note"
-- (Dine in / Takeaway / Delivery). Online orders keep order_type NULL and are not touched.
-- Safe to run more than once. Run it after deploying the API that has OrderType.

BEGIN;

ALTER TABLE orders ADD COLUMN IF NOT EXISTS order_type varchar(20);

-- POS orders are the ones without a customer account. The extra checks keep out any online delivery order
-- whose customer link was lost.
CREATE TEMP TABLE pos_orders_without_type ON COMMIT DROP AS
SELECT id
FROM orders
WHERE customer_id IS NULL
  AND order_type IS NULL
  AND COALESCE(fulfillment_method, 'PICKUP') <> 'DELIVERY'
  AND delivery_latitude IS NULL;

-- Dine in: seated at a table, or noted "Dine in".
UPDATE orders
SET order_type = 'DINE_IN', fulfillment_method = 'DINE_IN'
WHERE id IN (SELECT id FROM pos_orders_without_type)
  AND (fulfillment_method = 'DINE_IN' OR lower(btrim(note)) = 'dine in');

-- Take away: "Takeaway", the old "Delivery" label, and orders with no note (they were saved as PICKUP).
UPDATE orders
SET order_type = 'TAKE_AWAY', fulfillment_method = 'PICKUP'
WHERE id IN (SELECT id FROM pos_orders_without_type)
  AND order_type IS NULL;

-- The labels only stood in for the type; clear them so they don't show as a note.
UPDATE orders
SET note = NULL
WHERE order_type IS NOT NULL
  AND lower(btrim(note)) IN ('dine in', 'takeaway', 'take away', 'delivery');

-- POS orders are never delivered: no delivery fee.
UPDATE orders
SET delivery_fee = NULL, delivery_fee_set_at = NULL
WHERE order_type IS NOT NULL
  AND (delivery_fee IS NOT NULL OR delivery_fee_set_at IS NOT NULL);

COMMIT;

-- Check: every POS order now has a type.
SELECT order_type, fulfillment_method, count(*)
FROM orders
WHERE customer_id IS NULL
GROUP BY order_type, fulfillment_method
ORDER BY order_type, fulfillment_method;
