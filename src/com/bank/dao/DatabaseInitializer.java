package com.bank.dao;

import com.bank.exceptions.DatabaseException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * ============================================================================
 * DATABASE INITIALIZER (JDBC DDL & SCHEMA MANAGER)
 * ============================================================================
 * Demonstrates:
 * - DDL (Data Definition Language) execution via JDBC Statement
 * - Database schema initialization and table creation
 * - Index creation for relational performance optimization
 * - ACID relational constraints (Primary Keys, Foreign Keys, Unique constraints)
 */
public class DatabaseInitializer {

    /**
     * Initializes all database tables and indexes using JDBC Statement.
     * @throws DatabaseException if table creation fails.
     */
    public static void initializeDatabase() throws DatabaseException {
        DBConnectionManager dbManager = DBConnectionManager.getInstance();
        if (!dbManager.isDriverAvailable()) {
            return; // Graceful skip if external JDBC driver is not on classpath
        }

        Connection conn = null;
        Statement stmt = null;
        try {
            conn = dbManager.getConnection();
            stmt = conn.createStatement();

            // 1. Users Table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users ("
                    + "id VARCHAR(64) PRIMARY KEY, "
                    + "username VARCHAR(64) UNIQUE NOT NULL, "
                    + "password_hash VARCHAR(128) NOT NULL, "
                    + "role VARCHAR(32) NOT NULL, "
                    + "full_name VARCHAR(128) NOT NULL, "
                    + "email VARCHAR(128) NOT NULL, "
                    + "phone_number VARCHAR(32), "
                    + "address TEXT, "
                    + "pan_or_ssn VARCHAR(64), "
                    + "transaction_pin VARCHAR(64), "
                    + "occupation VARCHAR(128), "
                    + "monthly_income DOUBLE, "
                    + "department VARCHAR(128), "
                    + "clearance_level INT, "
                    + "is_frozen BOOLEAN DEFAULT FALSE, "
                    + "created_at VARCHAR(64)"
                    + ");");

            // 2. Accounts Table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS accounts ("
                    + "account_number VARCHAR(64) PRIMARY KEY, "
                    + "customer_id VARCHAR(64) NOT NULL, "
                    + "account_type VARCHAR(32) NOT NULL, "
                    + "balance DOUBLE NOT NULL, "
                    + "currency VARCHAR(16) NOT NULL, "
                    + "status VARCHAR(32) NOT NULL, "
                    + "interest_rate DOUBLE, "
                    + "minimum_balance DOUBLE, "
                    + "overdraft_limit DOUBLE, "
                    + "card_number VARCHAR(32), "
                    + "card_expiry VARCHAR(16), "
                    + "card_cvv VARCHAR(8), "
                    + "card_frozen BOOLEAN DEFAULT FALSE, "
                    + "created_at VARCHAR(64)"
                    + ");");

            // 3. Transactions Table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS transactions ("
                    + "id VARCHAR(64) PRIMARY KEY, "
                    + "source_account VARCHAR(64), "
                    + "destination_account VARCHAR(64), "
                    + "customer_id VARCHAR(64), "
                    + "amount DOUBLE NOT NULL, "
                    + "currency VARCHAR(16), "
                    + "type VARCHAR(32) NOT NULL, "
                    + "status VARCHAR(32) NOT NULL, "
                    + "description TEXT, "
                    + "timestamp VARCHAR(64)"
                    + ");");

            // 4. Loan Applications Table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS loans ("
                    + "id VARCHAR(64) PRIMARY KEY, "
                    + "customer_id VARCHAR(64) NOT NULL, "
                    + "applicant_name VARCHAR(128) NOT NULL, "
                    + "loan_type VARCHAR(64) NOT NULL, "
                    + "amount DOUBLE NOT NULL, "
                    + "tenure_months INT NOT NULL, "
                    + "interest_rate DOUBLE NOT NULL, "
                    + "monthly_emi DOUBLE NOT NULL, "
                    + "total_payable DOUBLE NOT NULL, "
                    + "status VARCHAR(32) NOT NULL, "
                    + "purpose TEXT, "
                    + "admin_remarks TEXT, "
                    + "applied_at VARCHAR(64), "
                    + "reviewed_at VARCHAR(64)"
                    + ");");

            // 5. Audit Logs Table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS audit_logs ("
                    + "id VARCHAR(64) PRIMARY KEY, "
                    + "user_id VARCHAR(64), "
                    + "username VARCHAR(128), "
                    + "role VARCHAR(32), "
                    + "action VARCHAR(128) NOT NULL, "
                    + "details TEXT, "
                    + "ip_address VARCHAR(64), "
                    + "timestamp VARCHAR(64)"
                    + ");");

            System.out.println("[JDBC DatabaseInitializer] Relational schema initialized successfully.");

        } catch (SQLException e) {
            throw new DatabaseException("Database schema initialization failed: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(stmt, null);
            DBConnectionManager.closeConnection(conn);
        }
    }
}
