UPDATE product_size_options
SET name = upper(name)
WHERE upper(name) IN ('SMALL', 'MEDIUM', 'LARGE') AND name <> upper(name);

UPDATE order_items
SET size_option_name = upper(size_option_name)
WHERE size_option_name IS NOT NULL
  AND upper(size_option_name) IN ('SMALL', 'MEDIUM', 'LARGE')
  AND size_option_name <> upper(size_option_name);