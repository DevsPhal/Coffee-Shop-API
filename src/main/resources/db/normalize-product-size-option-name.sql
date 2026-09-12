-- ProductSizeOption.name (and its snapshot on OrderItem.sizeOptionName) changed from free-text
-- String to the ProductSize enum (SMALL/MEDIUM/LARGE) — see common/enums/ProductSize.java.
-- Existing rows may hold arbitrary free text ("Small", "sm", "Regular", ...) that Hibernate
-- cannot deserialize into the enum: reading a row whose name isn't exactly one of the enum names
-- throws on every query that touches it — the same class of failure fixed in
-- add-product-sell-unit-columns.sql / normalize-product-stock-unit.sql.
--
-- Safe to run more than once. Run against every database that already has these tables: local
-- dev DB and production. Run this BEFORE deploying the code that maps these columns to
-- ProductSize — otherwise every request touching size options or past orders fails.
--
-- Only exact case-insensitive matches of SMALL/MEDIUM/LARGE are mapped automatically. Anything
-- else (e.g. "Regular", "XL") has no safe automatic mapping — inspect and fix those rows by hand
-- before/after running this, e.g.:
--   SELECT id, product_id, name FROM product_size_options
--     WHERE upper(name) NOT IN ('SMALL', 'MEDIUM', 'LARGE');
--   UPDATE product_size_options SET name = 'LARGE' WHERE id = '...';

UPDATE product_size_options
SET name = upper(name)
WHERE upper(name) IN ('SMALL', 'MEDIUM', 'LARGE') AND name <> upper(name);

UPDATE order_items
SET size_option_name = upper(size_option_name)
WHERE size_option_name IS NOT NULL
  AND upper(size_option_name) IN ('SMALL', 'MEDIUM', 'LARGE')
  AND size_option_name <> upper(size_option_name);
