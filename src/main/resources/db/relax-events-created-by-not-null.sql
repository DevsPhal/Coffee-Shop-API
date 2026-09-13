-- Event.createdBy changed from a required plain UUID to a nullable @ManyToOne Admin (see
-- CurrentActor.adminRef()) — null now means "created by the Super Admin", which deliberately has
-- no row in "admins" to reference (see SuperAdminUserDetails). The column's existing NOT NULL
-- would reject exactly that case, and ddl-auto: update never relaxes a constraint it no longer
-- generates.
--
-- Safe to run more than once. Run against every database that already has this table: local dev
-- DB and production. Run this BEFORE deploying the code that makes the field nullable.

ALTER TABLE events ALTER COLUMN created_by DROP NOT NULL;
