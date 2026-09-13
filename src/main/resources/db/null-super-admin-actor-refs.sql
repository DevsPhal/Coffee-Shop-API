-- Product.createdBy/updatedBy, Category.createdBy/updatedBy, Event.createdBy,
-- Banner.adminId/updatedByAdminId, BakongExchangeRate.updatedByAdminId, and Barista.createdBy
-- changed from a plain UUID to a real nullable @ManyToOne Admin — see CurrentActor.adminRef().
-- Any existing row where one of these holds the Super Admin's fixed id (or any other id with no
-- matching "admins" row) would violate the FK ddl-auto: update is about to add, since the Super
-- Admin deliberately has no row there (see SuperAdminUserDetails). Null is the correct value for
-- exactly that case going forward, so this just retroactively applies the same rule to history.
--
-- Safe to run more than once. Run against every database that already has these tables: local
-- dev DB and production. Run this BEFORE deploying the code that adds the relations — otherwise
-- ddl-auto: update's ALTER TABLE ... ADD CONSTRAINT fails outright on any violating row.

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
