-- orders.barista_id/handled_by_admin/handled_by_barista and order_audit_logs.actor_admin_id/
-- actor_barista_id/actor_customer_id are leftovers from an earlier design that split "who did
-- this" into one nullable FK column per possible role, before both entities were collapsed to a
-- single plain UUID (Order.handledBy, OrderAuditLog.actorId) resolved at read time via
-- ActorLookupService — see those fields' own javadoc for why a real FK doesn't work here (the
-- Super Admin has a fixed synthetic id with no row in any table). ddl-auto: update never drops a
-- column it no longer generates, so these six stayed behind, entirely unwritten, ever since.
--
-- Safe to run more than once. Run against every database that already has these tables: local
-- dev DB and production. Purely additive cleanup — nothing reads or writes these columns today.

ALTER TABLE orders DROP COLUMN IF EXISTS barista_id;
ALTER TABLE orders DROP COLUMN IF EXISTS handled_by_admin;
ALTER TABLE orders DROP COLUMN IF EXISTS handled_by_barista;

ALTER TABLE order_audit_logs DROP COLUMN IF EXISTS actor_admin_id;
ALTER TABLE order_audit_logs DROP COLUMN IF EXISTS actor_barista_id;
ALTER TABLE order_audit_logs DROP COLUMN IF EXISTS actor_customer_id;
