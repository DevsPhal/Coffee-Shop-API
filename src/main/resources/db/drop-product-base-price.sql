INSERT INTO product_size_options (id, product_id, name, price_delta, sort_order, status, created_at, updated_at)
SELECT gen_random_uuid(), p.id, 'MEDIUM', p.price, 1, 'ACTIVE', now(), now()
FROM products p
WHERE NOT EXISTS (SELECT 1 FROM product_size_options pso WHERE pso.product_id = p.id);

UPDATE product_size_options pso
SET price_delta = pso.price_delta + p.price
FROM products p
WHERE p.id = pso.product_id;

ALTER TABLE product_size_options RENAME COLUMN price_delta TO price;

ALTER TABLE products DROP COLUMN IF EXISTS price;