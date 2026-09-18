UPDATE products SET created_by = NULL WHERE created_by IS NOT NULL AND created_by NOT IN (SELECT id FROM admins);
UPDATE products SET updated_by = NULL WHERE updated_by IS NOT NULL AND updated_by NOT IN (SELECT id FROM admins);

UPDATE categories SET created_by = NULL WHERE created_by IS NOT NULL AND created_by NOT IN (SELECT id FROM admins);
UPDATE categories SET updated_by = NULL WHERE updated_by IS NOT NULL AND updated_by NOT IN (SELECT id FROM admins);

UPDATE events SET created_by = NULL WHERE created_by IS NOT NULL AND created_by NOT IN (SELECT id FROM admins);

UPDATE banners SET admin_id = NULL WHERE admin_id IS NOT NULL AND admin_id NOT IN (SELECT id FROM admins);
UPDATE banners SET updated_by_admin_id = NULL WHERE updated_by_admin_id IS NOT NULL AND updated_by_admin_id NOT IN (SELECT id FROM admins);

UPDATE bakong_exchange_rate SET updated_by_admin_id = NULL
    WHERE updated_by_admin_id IS NOT NULL AND updated_by_admin_id NOT IN (SELECT id FROM admins);

UPDATE baristas SET created_by = NULL WHERE created_by IS NOT NULL AND created_by NOT IN (SELECT id FROM admins);