UPDATE order_items oi
SET size_option_id = pso.id
FROM product_size_options pso
WHERE pso.product_id = oi.product_id
  AND upper(pso.name) = upper(oi.size_option_name)
  AND oi.size_option_name IS NOT NULL
  AND oi.size_option_id IS NULL;

ALTER TABLE order_items DROP COLUMN IF EXISTS size_option_name;