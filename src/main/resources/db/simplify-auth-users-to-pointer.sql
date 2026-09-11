-- Simplifies auth_users from a duplicated copy of every Admin/Barista/Customer column into a
-- thin role-indexed pointer at whichever of those tables actually owns the row (see AuthUser's
-- javadoc). Nothing in the app ever read these mirrored columns — the real, current data always
-- lived on admins/baristas/customers themselves — so dropping them loses nothing; `id` and `role`
-- are all that's left mapped by the entity now. With no migration tool, ddl-auto: update never
-- drops columns on its own, so this does it by hand; dropping a column also drops any check
-- constraint that references only it (e.g. the enum-value constraints from
-- fix-enum-check-constraints.sql), so nothing further to clean up there.
--
-- Safe to run more than once. Run against every database that already has this table: local dev
-- DB and production.
ALTER TABLE auth_users DROP COLUMN IF EXISTS full_name;
ALTER TABLE auth_users DROP COLUMN IF EXISTS email;
ALTER TABLE auth_users DROP COLUMN IF EXISTS password;
ALTER TABLE auth_users DROP COLUMN IF EXISTS phone_number;
ALTER TABLE auth_users DROP COLUMN IF EXISTS gender;
ALTER TABLE auth_users DROP COLUMN IF EXISTS status;
ALTER TABLE auth_users DROP COLUMN IF EXISTS telegram_chat_id;
ALTER TABLE auth_users DROP COLUMN IF EXISTS created_at;
ALTER TABLE auth_users DROP COLUMN IF EXISTS updated_at;
