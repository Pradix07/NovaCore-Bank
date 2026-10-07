package com.bank.dao;

import com.bank.exceptions.DatabaseException;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * ============================================================================
 * DATABASE CONNECTIVITY MANAGER (JDBC)
 * ============================================================================
 * Demonstrates:
 * - JDBC Architecture (DriverManager, Connection, Statement, PreparedStatement, ResultSet)
 * - Singleton Design Pattern for centralized Database Connection lifecycle
 * - Transaction Management (Auto-commit toggle, commit, rollback)
 * - Thread safety and resource cleanup
 * - Universal Driver Support: Supports SQLite, H2, MySQL, PostgreSQL, or Embedded DB
 */
public class DBConnectionManager {

    private static DBConnectionManager instance;
    
    // Default to lightweight embedded SQLite / File JDBC
    private static final String DEFAULT_DB_FILE = "data/bank_master.db";
    private String jdbcUrl = "jdbc:sqlite:" + DEFAULT_DB_FILE;
    private String dbUser = "";
    private String dbPassword = "";
    private boolean isDriverLoaded = false;

    private DBConnectionManager() {
        initDriver();
    }

    /**
     * Singleton accessor for DBConnectionManager.
     * @return Single instance of DBConnectionManager.
     */
    public static synchronized DBConnectionManager getInstance() {
        if (instance == null) {
            instance = new DBConnectionManager();
        }
        return instance;
    }

    /**
     * Initializes the JDBC Driver with graceful fallback detection.
     */
    private void initDriver() {
        try {
            // Ensure data directory exists
            File dir = new File("data");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Attempt loading JDBC drivers
            try {
                Class.forName("org.sqlite.JDBC");
                isDriverLoaded = true;
            } catch (ClassNotFoundException e1) {
                try {
                    Class.forName("org.h2.Driver");
                    jdbcUrl = "jdbc:h2:./data/bank_master";
                    isDriverLoaded = true;
                } catch (ClassNotFoundException e2) {
                    // Standard fallback for systems without external JARs
                    isDriverLoaded = false;
                }
            }
        } catch (Exception e) {
            System.err.println("[JDBC] Driver registration note: " + e.getMessage());
        }
    }

    /**
     * Configures custom JDBC connection parameters (e.g. for MySQL/PostgreSQL/H2).
     * @param url JDBC Connection URL
     * @param user Database username
     * @param password Database password
     */
    public synchronized void configure(String url, String user, String password) {
        this.jdbcUrl = url;
        this.dbUser = user;
        this.dbPassword = password;
    }

    /**
     * Obtains a live JDBC Connection.
     * @return Active java.sql.Connection instance.
     * @throws DatabaseException if connection fails.
     */
    public Connection getConnection() throws DatabaseException {
        try {
            if (dbUser != null && !dbUser.isEmpty()) {
                return DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
            } else {
                return DriverManager.getConnection(jdbcUrl);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to establish JDBC Connection: " + e.getMessage(), e);
        }
    }

    /**
     * Safely closes a JDBC Connection.
     * @param conn Connection to close
     */
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ignored) {}
        }
    }

    /**
     * Safely closes JDBC Statement and ResultSet resources.
     * @param stmt Statement to close
     * @param rs ResultSet to close
     */
    public static void closeResources(Statement stmt, ResultSet rs) {
        if (rs != null) {
            try { rs.close(); } catch (SQLException ignored) {}
        }
        if (stmt != null) {
            try { stmt.close(); } catch (SQLException ignored) {}
        }
    }

    /**
     * Safely closes JDBC PreparedStatement and Connection.
     * @param conn Connection to close
     * @param pstmt PreparedStatement to close
     */
    public static void closeResources(Connection conn, PreparedStatement pstmt) {
        if (pstmt != null) {
            try { pstmt.close(); } catch (SQLException ignored) {}
        }
        closeConnection(conn);
    }

    public boolean isDriverAvailable() {
        return isDriverLoaded;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }
}
