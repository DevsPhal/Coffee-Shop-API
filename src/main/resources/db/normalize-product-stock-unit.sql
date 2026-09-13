-- Product.stockUnit changed from a free-text String to the StockUnit enum (PACK/BOX/CARTON) —
-- see common/enums/StockUnit.java. Existing rows have arbitrary free text ("kg", "L", "cup",
-- "pack", ...) that Hibernate cannot deserialize into the enum: reading a product whose
-- stock_unit isn't exactly one of the enum names throws IllegalArgumentException on every query
-- that touches it, the same class of failure fixed in add-product-sell-unit-columns.sql when
-- sell_unit was missing outright. Uppercasing alone only fixes values that already happen to
-- match an enum name (e.g. "pack" -> "PACK"); anything else must be forced to a valid value
-- before the app can read that row again.
--
-- Safe to run more than once. Run against every database that already has a "products" table:
-- local dev DB and production. Run this BEFORE deploying the code that maps stock_unit to
-- StockUnit — otherwise every request touching products fails again, exactly like last time.
--
-- Values that don't already match PACK/BOX/CARTON fall back to 'PACK' below — review and correct
-- them per-product afterwards (e.g. via the product update endpoint):
--   SELECT sku, stock_unit FROM products; -- re-check after running
--   UPDATE products SET stock_unit = 'BOX' WHERE sku IN (...);

UPDATE products
SET stock_unit = CASE
    WHEN upper(stock_unit) IN ('PACK', 'BOX', 'CARTON') THEN upper(stock_unit)
    ELSE 'PACK'
END
WHERE stock_unit IS NOT NULL;
