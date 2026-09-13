-- SugarLevel/IceLevel changed from percentage-based (ZERO/TWENTY_FIVE/FIFTY/SEVENTY_FIVE/HUNDRED)
-- to level-based (SugarLevel: ZERO/LESS/NORMAL/EXTRA, IceLevel: NO_ICE/LESS_ICE/NORMAL/EXTRA_ICE),
-- and MilkType changed from a list of milk types (WHOLE_MILK/SKIM_MILK/...) to an amount
-- (NONE/LESS/NORMAL/EXTRA) — see common/enums/{SugarLevel,IceLevel,MilkType}.java.
--
-- cart_items and order_items both predate NoEnumCheckPostgreSQLDialect, so Hibernate generated
-- CHECK constraints against the old value lists on these three columns. Those constraints reject
-- every new value outright, and any existing row still holding an old value can no longer be
-- read back correctly by the app either way — the same class of failure fixed in
-- add-product-sell-unit-columns.sql / normalize-product-stock-unit.sql /
-- normalize-product-size-option-name.sql.
--
-- Safe to run more than once. Run against every database that already has these tables: local
-- dev DB and production. Run this BEFORE deploying the code that maps these columns to the new
-- enum values — otherwise every cart/order request touching a row with an old value fails, and
-- every write is rejected by the stale CHECK constraint.
--
-- Sugar/ice have a natural percentage -> level mapping. Milk type does not (it was never an
-- amount) — every non-NONE milk type below is mapped to NORMAL as a reasonable default; review
-- and correct affected orders by hand afterwards if the exact amount matters historically:
--   SELECT id, milk_type FROM order_items WHERE milk_type = 'NORMAL';

ALTER TABLE cart_items DROP CONSTRAINT IF EXISTS cart_items_sugar_level_check;
ALTER TABLE cart_items DROP CONSTRAINT IF EXISTS cart_items_ice_level_check;
ALTER TABLE cart_items DROP CONSTRAINT IF EXISTS cart_items_milk_type_check;
ALTER TABLE order_items DROP CONSTRAINT IF EXISTS order_items_sugar_level_check;
ALTER TABLE order_items DROP CONSTRAINT IF EXISTS order_items_ice_level_check;
ALTER TABLE order_items DROP CONSTRAINT IF EXISTS order_items_milk_type_check;

UPDATE cart_items SET sugar_level = CASE sugar_level
    WHEN 'ZERO' THEN 'ZERO'
    WHEN 'TWENTY_FIVE' THEN 'LESS'
    WHEN 'FIFTY' THEN 'NORMAL'
    WHEN 'SEVENTY_FIVE' THEN 'EXTRA'
    WHEN 'HUNDRED' THEN 'EXTRA'
    ELSE sugar_level
END WHERE sugar_level IS NOT NULL;

UPDATE cart_items SET ice_level = CASE ice_level
    WHEN 'ZERO' THEN 'NO_ICE'
    WHEN 'TWENTY_FIVE' THEN 'LESS_ICE'
    WHEN 'FIFTY' THEN 'NORMAL'
    WHEN 'SEVENTY_FIVE' THEN 'EXTRA_ICE'
    WHEN 'HUNDRED' THEN 'EXTRA_ICE'
    ELSE ice_level
END WHERE ice_level IS NOT NULL;

UPDATE cart_items SET milk_type = CASE milk_type
    WHEN 'NONE' THEN 'NONE'
    WHEN 'LESS' THEN 'LESS'
    WHEN 'NORMAL' THEN 'NORMAL'
    WHEN 'EXTRA' THEN 'EXTRA'
    ELSE 'NORMAL'
END WHERE milk_type IS NOT NULL;

UPDATE order_items SET sugar_level = CASE sugar_level
    WHEN 'ZERO' THEN 'ZERO'
    WHEN 'TWENTY_FIVE' THEN 'LESS'
    WHEN 'FIFTY' THEN 'NORMAL'
    WHEN 'SEVENTY_FIVE' THEN 'EXTRA'
    WHEN 'HUNDRED' THEN 'EXTRA'
    ELSE sugar_level
END WHERE sugar_level IS NOT NULL;

UPDATE order_items SET ice_level = CASE ice_level
    WHEN 'ZERO' THEN 'NO_ICE'
    WHEN 'TWENTY_FIVE' THEN 'LESS_ICE'
    WHEN 'FIFTY' THEN 'NORMAL'
    WHEN 'SEVENTY_FIVE' THEN 'EXTRA_ICE'
    WHEN 'HUNDRED' THEN 'EXTRA_ICE'
    ELSE ice_level
END WHERE ice_level IS NOT NULL;

UPDATE order_items SET milk_type = CASE milk_type
    WHEN 'NONE' THEN 'NONE'
    WHEN 'LESS' THEN 'LESS'
    WHEN 'NORMAL' THEN 'NORMAL'
    WHEN 'EXTRA' THEN 'EXTRA'
    ELSE 'NORMAL'
END WHERE milk_type IS NOT NULL;
