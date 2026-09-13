-- OrderStatus gained new constants (PAID, PREPARING, OUT_FOR_DELIVERY, DELIVERED) for the
-- post-payment fulfillment lifecycle — same class of failure as fix-enum-check-constraints.sql /
-- drop-order-audit-log-action-check.sql. orders.status still has a CHECK constraint generated
-- before NoEnumCheckPostgreSQLDialect was in effect, listing only the old 3-value list
-- (PENDING/COMPLETED/CANCELLED). That constraint rejects every order the moment it's marked PAID.
--
-- Safe to run more than once. Run against every database that already has this table: local dev
-- DB and production. Run this BEFORE deploying the code that sets these new statuses — otherwise
-- every cash/Bakong payment confirmation fails.
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check;
