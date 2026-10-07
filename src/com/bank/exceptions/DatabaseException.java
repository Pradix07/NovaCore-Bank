package com.bank.exceptions;

/**
 * Custom checked exception representing JDBC / SQL database errors.
 * Demonstrates:
 * - OOP Principle: Exception Hierarchy & Custom Checked Exceptions
 */
public class DatabaseException extends BankingException {
    
    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message + (cause != null ? ": " + cause.getMessage() : ""));
        if (cause != null) {
            initCause(cause);
        }
    }
}
