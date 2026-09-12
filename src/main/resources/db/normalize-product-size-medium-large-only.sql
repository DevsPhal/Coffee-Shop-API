-- ProductSize dropped SMALL — now just MEDIUM/LARGE (see common/enums/ProductSize.java). Any
-- existing SMALL row on product_size_options.name (or its order_items.size_option_name snapshot)
-- can no longer be read back by the app; same class of failure fixed in
-- normalize-product-size-option-name.sql when the enum was first introduced.
--
-- Safe to run more than once. Run against every database that already has these tables: local
-- dev DB and production. Run this BEFORE deploying the code that drops SMALL — otherwise every
-- request touching a SMALL row fails.
--
-- SMALL collapses into MEDIUM (the new floor size). This can only conflict with the
-- (product_id, name) unique constraint on product_size_options if some product already has BOTH
-- a SMALL and a MEDIUM row — the first UPDATE below is written to skip exactly that case instead
-- of erroring, so check its output: any product still showing SMALL afterwards needs a manual
-- decision (delete one of the two rows, or keep SMALL renamed to something else entirely).
--   SELECT product_id, name FROM product_size_options WHERE name = 'SMALL';

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
