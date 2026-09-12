-- admins.created_by, baristas.created_by, categories.created_by/updated_by,
-- events.created_by, products.created_by/updated_by, and
-- banners.admin_id/updated_by_admin_id are audit-style columns resolved at
-- read time via ActorLookupService (the creator/updater can be the Super
-- Admin, who has no row in "admins"). Hibernate generated these as real
-- foreign keys back when the fields were mapped as @ManyToOne Admin; the
-- entities were later changed to plain UUID columns, but ddl-auto: update
-- never drops constraints it no longer generates, so the stale FKs stayed
-- behind and reject any row whose value is the Super Admin's fixed id
-- (00000000-0000-0000-0000-000000000001) since that id was never present in
-- "admins". expenses.recorded_by had the same issue, but that whole table is
-- gone now — see drop-legacy-expenses-table.sql.
ALTER TABLE admins DROP CONSTRAINT IF EXISTS fkqu7et826jq36a9ddcfa0eswbk;
ALTER TABLE baristas DROP CONSTRAINT IF EXISTS fkiwby13w8p39c61nwpv2mfpp8d;
ALTER TABLE banners DROP CONSTRAINT IF EXISTS fkb6s6fviic9s9tt2tq6m11qb32;
ALTER TABLE banners DROP CONSTRAINT IF EXISTS fkn6g3tn9mbtoy101rymjcclhgy;
ALTER TABLE categories DROP CONSTRAINT IF EXISTS fkntqftnej11qykg21idig2eje4;
ALTER TABLE categories DROP CONSTRAINT IF EXISTS fkjt9biwo3xw1ktk4on738n3c25;
ALTER TABLE events DROP CONSTRAINT IF EXISTS fksfyevhq3kda9tysjyf0ivmjnp;
ALTER TABLE products DROP CONSTRAINT IF EXISTS fkqwyo518c4odqn4gcj1kskicwj;
ALTER TABLE products DROP CONSTRAINT IF EXISTS fk1soskk57373o938efjh8g5km0;
