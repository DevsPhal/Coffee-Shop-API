package org.group1.coffeeshopapi.table.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

// Database rules Hibernate can't create on its own (PostgreSQL only; the same SQL is in
// db/add-table-reservations.sql). The services already lock the table row; these back that up:
//  - an old CHECK constraint on dining_tables.status would reject the new CLEANING status;
//  - a table can have at most one seated booking at a time.
@Slf4j
@Component
@RequiredArgsConstructor
public class TableSchemaGuard {

    static final String DROP_STATUS_CHECKS = """
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
                END LOOP;
            END $$""";

    static final String ONE_SEATED_BOOKING_PER_TABLE = """
            CREATE UNIQUE INDEX IF NOT EXISTS ux_reservations_one_seated_per_table
                ON reservations (table_id) WHERE status = 'SEATED'""";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void apply() {
        try {
            if (!isPostgres()) {
                return;
            }
            jdbcTemplate.execute(DROP_STATUS_CHECKS);
            jdbcTemplate.execute(ONE_SEATED_BOOKING_PER_TABLE);
        } catch (Exception e) {
            // Never stop the app over this: the services enforce the same rules with row locks.
            log.warn("Could not apply the table database rules; run db/add-table-reservations.sql by hand", e);
        }
    }

    private boolean isPostgres() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            return "PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName());
        }
    }
}
