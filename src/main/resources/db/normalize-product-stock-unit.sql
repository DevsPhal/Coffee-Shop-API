UPDATE products
SET stock_unit = CASE
    WHEN upper(stock_unit) IN ('PACK', 'BOX', 'CARTON') THEN upper(stock_unit)
    ELSE 'PACK'
END
WHERE stock_unit IS NOT NULL;