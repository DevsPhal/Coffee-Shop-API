package org.group1.coffeeshopapi.config;

import org.hibernate.dialect.PostgreSQLDialect;

/**
 * Hibernate auto-generates a CHECK constraint for every {@code @Enumerated(STRING)} column from
 * whatever the enum's constants are at the moment the table is first created. With
 * {@code ddl-auto: update} (no Flyway/Liquibase in this project), that constraint is never
 * revisited afterward — renaming or adding an enum constant silently leaves the database
 * enforcing the old set, so a perfectly valid request starts failing at the DB layer with no
 * corresponding code change to blame. Bean Validation and JSON deserialization already reject
 * unknown enum values before they reach persistence, so the DB-level check adds no real safety
 * here, only drift risk. Disabling it stops this class of outage from happening again.
 */
public class NoEnumCheckPostgreSQLDialect extends PostgreSQLDialect {
    @Override
    public boolean supportsColumnCheck() {
        return false;
    }
}
