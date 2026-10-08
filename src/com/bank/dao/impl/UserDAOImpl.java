package com.bank.dao.impl;

import com.bank.dao.DBConnectionManager;
import com.bank.dao.UserDAO;
import com.bank.exceptions.DatabaseException;
import com.bank.model.Admin;
import com.bank.model.Customer;
import com.bank.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * CLASS: UserDAOImpl (JDBC Implementation)
 * ============================================================================
 * Demonstrates:
 * - Interface Implementation (implements UserDAO)
 * - JDBC PreparedStatement usage to prevent SQL Injection
 * - Polymorphic object creation from ResultSet (Customer vs Admin)
 * - Safe Database Resource Management
 */
public class UserDAOImpl implements UserDAO {

    private final DBConnectionManager dbManager = DBConnectionManager.getInstance();

    @Override
    public boolean save(User user) throws DatabaseException {
        String sql = "INSERT INTO users (id, username, password_hash, role, full_name, email, phone_number, "
                + "address, pan_or_ssn, aadhaar_number, upi_id, transaction_pin, occupation, monthly_income, department, clearance_level, is_frozen, created_at, last_login) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, user.getId());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getPasswordHash());
            pstmt.setString(4, user.getRole());
            pstmt.setString(5, user.getFullName());
            pstmt.setString(6, user.getEmail());
            pstmt.setString(7, user.getPhone());

            if (user instanceof Customer) {
                Customer c = (Customer) user;
                pstmt.setString(8, c.getAddress());
                pstmt.setString(9, c.getPanOrTaxId());
                pstmt.setString(10, c.getAadhaarNumber());
                pstmt.setString(11, c.getUpiId());
                pstmt.setString(12, c.getSecurityPin());
                pstmt.setString(13, c.getOccupation());
                pstmt.setDouble(14, c.getMonthlyIncome());
                pstmt.setString(15, null);
                pstmt.setInt(16, 0);
            } else if (user instanceof Admin) {
                Admin a = (Admin) user;
                pstmt.setString(8, null);
                pstmt.setString(9, null);
                pstmt.setString(10, null);
                pstmt.setString(11, null);
                pstmt.setString(12, null);
                pstmt.setString(13, null);
                pstmt.setDouble(14, 0.0);
                pstmt.setString(15, a.getDepartment());
                pstmt.setInt(16, a.getAccessLevel());
            } else {
                pstmt.setString(8, null);
                pstmt.setString(9, null);
                pstmt.setString(10, null);
                pstmt.setString(11, null);
                pstmt.setString(12, null);
                pstmt.setString(13, null);
                pstmt.setDouble(14, 0.0);
                pstmt.setString(15, null);
                pstmt.setInt(16, 0);
            }

            pstmt.setBoolean(17, !user.isActive());
            pstmt.setString(18, user.getCreatedAt());
            pstmt.setString(19, user.getLastLogin());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException("Error inserting user into database: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(conn, pstmt);
        }
    }

    @Override
    public Optional<User> findById(String id) throws DatabaseException {
        String sql = "SELECT * FROM users WHERE id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, id);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToUser(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Error finding user by ID: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(pstmt, rs);
            DBConnectionManager.closeConnection(conn);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) throws DatabaseException {
        String sql = "SELECT * FROM users WHERE LOWER(username) = LOWER(?)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToUser(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Error finding user by username: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(pstmt, rs);
            DBConnectionManager.closeConnection(conn);
        }
    }

    @Override
    public boolean existsByUsername(String username) throws DatabaseException {
        return findByUsername(username).isPresent();
    }

    @Override
    public List<User> findAll() throws DatabaseException {
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        List<User> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToUser(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching all users: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(pstmt, rs);
            DBConnectionManager.closeConnection(conn);
        }
    }

    @Override
    public boolean update(User user) throws DatabaseException {
        String sql = "UPDATE users SET full_name = ?, email = ?, phone_number = ?, is_frozen = ?, password_hash = ?, "
                + "address = ?, pan_or_ssn = ?, aadhaar_number = ?, upi_id = ?, transaction_pin = ?, occupation = ?, monthly_income = ?, "
                + "department = ?, clearance_level = ?, last_login = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, user.getFullName());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getPhone());
            pstmt.setBoolean(4, !user.isActive());
            pstmt.setString(5, user.getPasswordHash());

            if (user instanceof Customer) {
                Customer c = (Customer) user;
                pstmt.setString(6, c.getAddress());
                pstmt.setString(7, c.getPanOrTaxId());
                pstmt.setString(8, c.getAadhaarNumber());
                pstmt.setString(9, c.getUpiId());
                pstmt.setString(10, c.getSecurityPin());
                pstmt.setString(11, c.getOccupation());
                pstmt.setDouble(12, c.getMonthlyIncome());
                pstmt.setString(13, null);
                pstmt.setInt(14, 0);
            } else if (user instanceof Admin) {
                Admin a = (Admin) user;
                pstmt.setString(6, null);
                pstmt.setString(7, null);
                pstmt.setString(8, null);
                pstmt.setString(9, null);
                pstmt.setString(10, null);
                pstmt.setString(11, null);
                pstmt.setDouble(12, 0.0);
                pstmt.setString(13, a.getDepartment());
                pstmt.setInt(14, a.getAccessLevel());
            } else {
                pstmt.setString(6, null);
                pstmt.setString(7, null);
                pstmt.setString(8, null);
                pstmt.setString(9, null);
                pstmt.setString(10, null);
                pstmt.setString(11, null);
                pstmt.setDouble(12, 0.0);
                pstmt.setString(13, null);
                pstmt.setInt(14, 0);
            }

            pstmt.setString(15, user.getLastLogin());
            pstmt.setString(16, user.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating user: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(conn, pstmt);
        }
    }

    @Override
    public boolean setUserFrozenStatus(String userId, boolean isFrozen) throws DatabaseException {
        String sql = "UPDATE users SET is_frozen = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setBoolean(1, isFrozen);
            pstmt.setString(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error setting frozen status: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(conn, pstmt);
        }
    }

    @Override
    public boolean deleteById(String id) throws DatabaseException {
        String sql = "DELETE FROM users WHERE id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting user: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(conn, pstmt);
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        String role = rs.getString("role");
        String id = rs.getString("id");
        String username = rs.getString("username");
        String passwordHash = rs.getString("password_hash");
        String fullName = rs.getString("full_name");
        String email = rs.getString("email");
        String phone = rs.getString("phone_number");
        boolean isFrozen = rs.getBoolean("is_frozen");
        String createdAt = rs.getString("created_at");
        String lastLogin = rs.getString("last_login");

        if ("ADMIN".equalsIgnoreCase(role)) {
            String department = rs.getString("department");
            int clearance = rs.getInt("clearance_level");
            Admin admin = new Admin(id, username, passwordHash, fullName, email, phone, department, clearance);
            admin.setActive(!isFrozen);
            if (createdAt != null) admin.setCreatedAt(createdAt);
            if (lastLogin != null) admin.setLastLogin(lastLogin);
            return admin;
        } else {
            String address = rs.getString("address");
            String pan = rs.getString("pan_or_ssn");
            String pin = rs.getString("transaction_pin");
            String aadhaar = rs.getString("aadhaar_number");
            String upi = rs.getString("upi_id");
            Customer customer = new Customer(id, username, passwordHash, fullName, email, phone, address, pan, aadhaar, upi, pin);
            customer.setOccupation(rs.getString("occupation"));
            customer.setMonthlyIncome(rs.getDouble("monthly_income"));
            customer.setActive(!isFrozen);
            if (createdAt != null) customer.setCreatedAt(createdAt);
            if (lastLogin != null) customer.setLastLogin(lastLogin);
            return customer;
        }
    }
}
