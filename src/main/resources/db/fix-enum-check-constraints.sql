-- Drops the stale Hibernate-generated CHECK constraints left behind after Gender (ANOTHER ->
-- OTHER) and User.status (Status -> UserStatus) changed. ddl-auto: update never updates existing
-- CHECK constraints, so these tables are still enforcing the old enum values and reject any
-- request using the new ones (e.g. gender=OTHER, or a fresh registration's status=
-- PENDING_VERIFICATION). See NoEnumCheckPostgreSQLDialect for why Hibernate will no longer
-- recreate constraints like these going forward.
--
-- Safe to run more than once. Does not touch any row data — only removes the constraint.
-- Run this against every database that already has these tables: local dev DB and production.

DO $$
DECLARE
    target RECORD;
    con    RECORD;
BEGIN
    FOR target IN
        SELECT * FROM (VALUES
            ('customers', 'gender'),
            ('customers', 'status'),
            ('baristas', 'gender'),
            ('baristas', 'status'),
            ('admins', 'gender'),
            ('admins', 'status'),
            ('auth_users', 'gender'),
            ('auth_users', 'status')
        ) AS t(table_name, column_name)
    LOOP
        FOR con IN
            SELECT pgc.conname
            FROM pg_constraint pgc
            JOIN pg_class rel ON rel.oid = pgc.conrelid
            JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY (pgc.conkey)
            WHERE pgc.contype = 'c'
              AND rel.relname = target.table_name
              AND att.attname = target.column_name
        LOOP
            EXECUTE format('ALTER TABLE %I DROP CONSTRAINT %I', target.table_name, con.conname);
            RAISE NOTICE 'Dropped % on %.%', con.conname, target.table_name, target.column_name;
        END LOOP;
    END LOOP;
END $$;
