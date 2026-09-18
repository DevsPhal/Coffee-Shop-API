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