package org.group1.coffeeshopapi.config;

import org.hibernate.dialect.PostgreSQLDialect;

public class NoEnumCheckPostgreSQLDialect extends PostgreSQLDialect {
    @Override
    public boolean supportsColumnCheck() {
        return false;
    }
}
