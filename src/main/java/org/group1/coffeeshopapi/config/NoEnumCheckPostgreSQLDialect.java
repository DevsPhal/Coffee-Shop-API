package org.group1.coffeeshopapi.config;

import org.hibernate.dialect.PostgreSQLDialect;

// Disables Hibernate's auto-generated CHECK constraint on enum columns. We have no migrations,
// so that constraint never updates when an enum changes, and old values start getting rejected.
public class NoEnumCheckPostgreSQLDialect extends PostgreSQLDialect {
    @Override
    public boolean supportsColumnCheck() {
        return false;
    }
}
