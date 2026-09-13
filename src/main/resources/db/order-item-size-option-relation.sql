-- OrderItem.sizeOptionName (a plain snapshot string) is replaced by a live sizeOption relation —
-- see OrderItem.java. Unlike the old snapshot, a ProductSizeOption can no longer be deleted while
-- any order still references it (GlobalExceptionHandler now turns that delete attempt into a
-- clean 409 instead of a raw FK violation), so the relation can't dangle going forward. The
-- trade-off, accepted deliberately: renaming a size option now changes how past orders display it,
-- where before they kept the name as it was at sale time.
--
-- Run this AFTER deploying the code that adds OrderItem.sizeOption — ddl-auto: update will have
-- already created the new, initially-empty size_option_id column by then. Safe to run more than
-- once.
--
-- Step 1: best-effort backfill by matching each row's old name back to a size option on the same
-- product. Rows whose original size option was already deleted/renamed before this migration
-- existed (rare — dropping a referenced size option was possible before the FK guard above) have
-- no safe automatic match and are left NULL; find them with:
--   SELECT id, product_id, size_option_name FROM order_items
--     WHERE size_option_name IS NOT NULL AND size_option_id IS NULL;
UPDATE order_items oi
SET size_option_id = pso.id
FROM product_size_options pso
WHERE pso.product_id = oi.product_id
  AND upper(pso.name) = upper(oi.size_option_name)
  AND oi.size_option_name IS NOT NULL
  AND oi.size_option_id IS NULL;

-- Step 2: drop the now-unused snapshot column.
ALTER TABLE order_items DROP COLUMN IF EXISTS size_option_name;
