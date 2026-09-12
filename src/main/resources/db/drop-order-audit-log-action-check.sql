-- OrderAuditAction gained a new constant (DELIVERY_FEE_SET) for the delivery-fee-evaluation
-- feature. order_audit_logs predates NoEnumCheckPostgreSQLDialect, so Hibernate generated a CHECK
-- constraint against the old 4-value list on this column — same class of failure fixed in
-- fix-enum-check-constraints.sql / normalize-cart-order-item-variants.sql. That constraint
-- rejects every new DELIVERY_FEE_SET row outright.
--
-- Safe to run more than once. Run against every database that already has this table: local dev
-- DB and production. Run this BEFORE deploying the code that logs DELIVERY_FEE_SET — otherwise
-- every delivery-fee evaluation fails.

ALTER TABLE order_audit_logs DROP CONSTRAINT IF EXISTS order_audit_logs_action_check;
