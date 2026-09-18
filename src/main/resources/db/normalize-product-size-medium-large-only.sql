UPDATE product_size_options pso
SET name = 'MEDIUM'
WHERE pso.name = 'SMALL'
  AND NOT EXISTS (
      SELECT 1 FROM product_size_options existing
      WHERE existing.product_id = pso.product_id AND existing.name = 'MEDIUM'
  );

UPDATE order_items
SET size_option_name = 'MEDIUM'
WHERE size_option_name = 'SMALL';