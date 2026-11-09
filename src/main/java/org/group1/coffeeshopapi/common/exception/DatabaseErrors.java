package org.group1.coffeeshopapi.common.exception;

import java.sql.SQLException;

// Reads the Postgres SQL state behind a Spring data exception, so a service can tell a duplicate value
// apart from other database errors instead of guessing.
public final class DatabaseErrors {

    private static final String UNIQUE_VIOLATION = "23505";

    private DatabaseErrors() {
    }

    public static String sqlState(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException) {
                return sqlException.getSQLState();
            }
        }
        return null;
    }

    public static boolean isUniqueViolation(Throwable ex) {
        return UNIQUE_VIOLATION.equals(sqlState(ex));
    }
}
