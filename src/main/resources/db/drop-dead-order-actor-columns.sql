ALTER TABLE orders DROP COLUMN IF EXISTS barista_id;
ALTER TABLE orders DROP COLUMN IF EXISTS handled_by_admin;
ALTER TABLE orders DROP COLUMN IF EXISTS handled_by_barista;

ALTER TABLE order_audit_logs DROP COLUMN IF EXISTS actor_admin_id;
ALTER TABLE order_audit_logs DROP COLUMN IF EXISTS actor_barista_id;
ALTER TABLE order_audit_logs DROP COLUMN IF EXISTS actor_customer_id;