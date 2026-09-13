-- Product.price (the base price) is gone — a product is priced entirely through its
-- ProductSizeOption rows now (renamed from priceDelta to price, holding an absolute price
-- instead of an add-on). See common enum-driven work in this area and
-- product/service/ProductPriceResolver.java for why every product needs at least one size
-- option once this ships.
--
-- Safe to run more than once. Run against every database that already has these tables: local
-- dev DB and production. Run this BEFORE deploying the code that drops Product.price and renames
-- ProductSizeOption.priceDelta — otherwise every product/cart/order request fails the same way
-- prior migrations in this file describe (a column the entity expects no longer/not yet matches
-- what's in the database).
--
-- Step 1: any product with zero size options gets one default row (named MEDIUM) using its old
-- base price, so it isn't left completely unpriced.
INSERT INTO product_size_options (id, product_id, name, price_delta, sort_order, status, created_at, updated_at)
SELECT gen_random_uuid(), p.id, 'MEDIUM', p.price, 1, 'ACTIVE', now(), now()
FROM products p
WHERE NOT EXISTS (SELECT 1 FROM product_size_options pso WHERE pso.product_id = p.id);

-- Step 2: every existing size option's price_delta was an add-on to the product's base price —
-- convert it to the absolute price it now needs to represent before the column is renamed.
UPDATE product_size_options pso
SET price_delta = pso.price_delta + p.price
FROM products p
WHERE p.id = pso.product_id;

-- Step 3: rename now that its values are absolute prices, not deltas.
ALTER TABLE product_size_options RENAME COLUMN price_delta TO price;

-- Step 4: drop the base price column — nothing reads it anymore.
ALTER TABLE products DROP COLUMN IF EXISTS price;
