package com.bank.dao.impl;

import com.bank.dao.DBConnectionManager;
import com.bank.dao.TransactionDAO;
import com.bank.exceptions.DatabaseException;
import com.bank.model.Transaction;
import com.bank.model.TransactionType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * CLASS: TransactionDAOImpl (JDBC Implementation)
 * ============================================================================
 * Demonstrates:
 * - Data Access Object Implementation for Transactions
 * - Multithreading safety & SQL Transactions
 */
public class TransactionDAOImpl implements TransactionDAO {

    private final DBConnectionManager dbManager = DBConnectionManager.getInstance();

    @Override
    public boolean save(Transaction tx) throws DatabaseException {
        String sql = "INSERT INTO transactions (id, source_account, destination_account, customer_id, "
                + "amount, currency, type, status, description, timestamp) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, tx.getId());
            pstmt.setString(2, tx.getFromAccount());
            pstmt.setString(3, tx.getToAccount());
            pstmt.setString(4, tx.getCustomerId());
            pstmt.setDouble(5, tx.getAmount());
            pstmt.setString(6, "INR");
            pstmt.setString(7, tx.getType() != null ? tx.getType().name() : "TRANSFER");
            pstmt.setString(8, tx.getStatus());
            pstmt.setString(9, tx.getDescription());
            pstmt.setString(10, tx.getTimestamp());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException("Error saving transaction: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(conn, pstmt);
        }
    }

    @Override
    public Optional<Transaction> findById(String id) throws DatabaseException {
        String sql = "SELECT * FROM transactions WHERE id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, id);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToTransaction(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Error finding transaction by ID: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(pstmt, rs);
            DBConnectionManager.closeConnection(conn);
        }
    }

    @Override
    public List<Transaction> findByAccountNumber(String accountNumber, int limit) throws DatabaseException {
        String sql = "SELECT * FROM transactions WHERE source_account = ? OR destination_account = ? ORDER BY timestamp DESC LIMIT ?";
        List<Transaction> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, accountNumber);
            pstmt.setString(2, accountNumber);
            pstmt.setInt(3, limit > 0 ? limit : 50);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToTransaction(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error querying account transactions: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(pstmt, rs);
            DBConnectionManager.closeConnection(conn);
        }
    }

    @Override
    public List<Transaction> findByCustomerId(String customerId) throws DatabaseException {
        String sql = "SELECT * FROM transactions WHERE customer_id = ? ORDER BY timestamp DESC";
        List<Transaction> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, customerId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToTransaction(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error querying customer transactions: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(pstmt, rs);
            DBConnectionManager.closeConnection(conn);
        }
    }

    @Override
    public List<Transaction> findAll() throws DatabaseException {
        String sql = "SELECT * FROM transactions ORDER BY timestamp DESC";
        List<Transaction> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToTransaction(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching all transactions: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(pstmt, rs);
            DBConnectionManager.closeConnection(conn);
        }
    }

    @Override
    public boolean update(Transaction tx) throws DatabaseException {
        String sql = "UPDATE transactions SET status = ?, description = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, tx.getStatus());
            pstmt.setString(2, tx.getDescription());
            pstmt.setString(3, tx.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating transaction: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(conn, pstmt);
        }
    }

    @Override
    public boolean deleteById(String id) throws DatabaseException {
        String sql = "DELETE FROM transactions WHERE id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting transaction: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(conn, pstmt);
        }
    }

    private Transaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String src = rs.getString("source_account");
        String dst = rs.getString("destination_account");
        String cust = rs.getString("customer_id");
        double amt = rs.getDouble("amount");
        String typeStr = rs.getString("type");
        String status = rs.getString("status");
        String desc = rs.getString("description");
        String time = rs.getString("timestamp");

        TransactionType type = TransactionType.TRANSFER_OUT;
        try {
            if (typeStr != null) type = TransactionType.valueOf(typeStr);
        } catch (Exception ignored) {}

        Transaction tx = new Transaction(id, type, amt, src, dst, desc, 0.0, cust, "REF-" + id);
        tx.setStatus(status);
        tx.setTimestamp(time);
        return tx;
    }
}
