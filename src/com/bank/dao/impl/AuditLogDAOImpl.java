package com.bank.dao.impl;

import com.bank.dao.AuditLogDAO;
import com.bank.dao.DBConnectionManager;
import com.bank.exceptions.DatabaseException;
import com.bank.model.AuditLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * CLASS: AuditLogDAOImpl (JDBC Implementation)
 * ============================================================================
 * Demonstrates:
 * - Data Access Object implementation for Security Audit Logs via JDBC
 */
public class AuditLogDAOImpl implements AuditLogDAO {

    private final DBConnectionManager dbManager = DBConnectionManager.getInstance();

    @Override
    public boolean save(AuditLog log) throws DatabaseException {
        String sql = "INSERT INTO audit_logs (id, user_id, username, role, action, details, ip_address, timestamp) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, log.getId());
            pstmt.setString(2, log.getActorId());
            pstmt.setString(3, log.getActorName());
            pstmt.setString(4, log.getActorRole());
            pstmt.setString(5, log.getAction());
            pstmt.setString(6, log.getDetails());
            pstmt.setString(7, log.getIpAddress());
            pstmt.setString(8, log.getTimestamp());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException("Error saving audit log: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(conn, pstmt);
        }
    }

    @Override
    public Optional<AuditLog> findById(String id) throws DatabaseException {
        String sql = "SELECT * FROM audit_logs WHERE id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, id);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToAuditLog(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Error finding audit log: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(pstmt, rs);
            DBConnectionManager.closeConnection(conn);
        }
    }

    @Override
    public List<AuditLog> findRecentLogs(int limit) throws DatabaseException {
        String sql = "SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT ?";
        List<AuditLog> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, limit > 0 ? limit : 50);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToAuditLog(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching recent audit logs: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(pstmt, rs);
            DBConnectionManager.closeConnection(conn);
        }
    }

    @Override
    public List<AuditLog> findByUserId(String userId) throws DatabaseException {
        String sql = "SELECT * FROM audit_logs WHERE user_id = ? ORDER BY timestamp DESC";
        List<AuditLog> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, userId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToAuditLog(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching user audit logs: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(pstmt, rs);
            DBConnectionManager.closeConnection(conn);
        }
    }

    @Override
    public List<AuditLog> findAll() throws DatabaseException {
        String sql = "SELECT * FROM audit_logs ORDER BY timestamp DESC";
        List<AuditLog> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToAuditLog(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching all audit logs: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(pstmt, rs);
            DBConnectionManager.closeConnection(conn);
        }
    }

    @Override
    public boolean update(AuditLog entity) throws DatabaseException {
        // Audit logs are immutable records
        return false;
    }

    @Override
    public boolean deleteById(String id) throws DatabaseException {
        String sql = "DELETE FROM audit_logs WHERE id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting audit log: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(conn, pstmt);
        }
    }

    private AuditLog mapResultSetToAuditLog(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String actorId = rs.getString("user_id");
        String actorName = rs.getString("username");
        String actorRole = rs.getString("role");
        String action = rs.getString("action");
        String details = rs.getString("details");
        String ip = rs.getString("ip_address");
        String timestamp = rs.getString("timestamp");

        AuditLog log = new AuditLog(id, actorId, actorName, actorRole, action, details, ip);
        log.setTimestamp(timestamp);
        return log;
    }
}
