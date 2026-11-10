-- Table reservations and the CLEANING table status.
-- Hibernate (ddl-auto: update) creates the reservations and table_audit_logs tables itself. The API also runs
-- the two statements below at startup on PostgreSQL (TableSchemaGuard); this file is for running them by hand.

-- 1. An old CHECK constraint on dining_tables.status would reject the new CLEANING status.
DO $$
DECLARE con RECORD;
BEGIN
    FOR con IN
        SELECT pgc.conname FROM pg_constraint pgc
        JOIN pg_class rel ON rel.oid = pgc.conrelid
        JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY (pgc.conkey)
        WHERE pgc.contype = 'c' AND rel.relname = 'dining_tables' AND att.attname = 'status'
    LOOP
        EXECUTE format('ALTER TABLE dining_tables DROP CONSTRAINT %I', con.conname);
        RAISE NOTICE 'Dropped % on dining_tables.status', con.conname;
    END LOOP;
END $$;

-- 2. A table has at most one seated booking at a time.
CREATE UNIQUE INDEX IF NOT EXISTS ux_reservations_one_seated_per_table
    ON reservations (table_id) WHERE status = 'SEATED';

-- Tables that staff marked RESERVED by hand before bookings existed have no booking behind them.
-- They stay RESERVED until someone frees them on the Tables screen (allowed, since no booking holds them).
-- To list them:
--   SELECT table_number FROM dining_tables t WHERE status = 'RESERVED'
--     AND NOT EXISTS (SELECT 1 FROM reservations r WHERE r.table_id = t.id AND r.status = 'BOOKED');
