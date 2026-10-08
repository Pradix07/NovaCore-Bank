package com.bank.dao.impl;

import com.bank.dao.DBConnectionManager;
import com.bank.dao.LoanDAO;
import com.bank.exceptions.DatabaseException;
import com.bank.model.LoanApplication;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * CLASS: LoanDAOImpl (JDBC Implementation)
 * ============================================================================
 * Demonstrates:
 * - Data Access Object pattern for loan management
 * - Safe SQL queries and status updates
 */
public class LoanDAOImpl implements LoanDAO {

    private final DBConnectionManager dbManager = DBConnectionManager.getInstance();

    @Override
    public boolean save(LoanApplication loan) throws DatabaseException {
        String sql = "INSERT INTO loans (id, customer_id, applicant_name, loan_type, amount, tenure_months, "
                + "interest_rate, monthly_emi, total_payable, status, purpose, admin_remarks, applied_at, reviewed_at, remaining_principal, emis_paid) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, loan.getId());
            pstmt.setString(2, loan.getCustomerId());
            pstmt.setString(3, loan.getCustomerName());
            pstmt.setString(4, loan.getLoanType());
            pstmt.setDouble(5, loan.getAmount());
            pstmt.setInt(6, loan.getTenureMonths());
            pstmt.setDouble(7, loan.getInterestRate());
            pstmt.setDouble(8, loan.getMonthlyEmi());
            pstmt.setDouble(9, loan.getMonthlyEmi() * loan.getTenureMonths());
            pstmt.setString(10, loan.getStatus() != null ? loan.getStatus() : "PENDING");
            pstmt.setString(11, loan.getPurpose());
            pstmt.setString(12, loan.getRemarks());
            pstmt.setString(13, loan.getAppliedAt());
            pstmt.setString(14, loan.getDecidedAt());
            pstmt.setDouble(15, loan.getRemainingPrincipal() > 0 ? loan.getRemainingPrincipal() : loan.getAmount());
            pstmt.setInt(16, loan.getEmisPaid());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException("Error saving loan application: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(conn, pstmt);
        }
    }

    @Override
    public Optional<LoanApplication> findById(String id) throws DatabaseException {
        String sql = "SELECT * FROM loans WHERE id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, id);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToLoan(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Error finding loan by ID: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(pstmt, rs);
            DBConnectionManager.closeConnection(conn);
        }
    }

    @Override
    public List<LoanApplication> findByCustomerId(String customerId) throws DatabaseException {
        String sql = "SELECT * FROM loans WHERE customer_id = ? ORDER BY applied_at DESC";
        List<LoanApplication> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, customerId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToLoan(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error finding customer loans: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(pstmt, rs);
            DBConnectionManager.closeConnection(conn);
        }
    }

    @Override
    public List<LoanApplication> findByStatus(String status) throws DatabaseException {
        String sql = "SELECT * FROM loans WHERE UPPER(status) = UPPER(?) ORDER BY applied_at DESC";
        List<LoanApplication> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, status);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToLoan(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error finding loans by status: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(pstmt, rs);
            DBConnectionManager.closeConnection(conn);
        }
    }

    @Override
    public List<LoanApplication> findAll() throws DatabaseException {
        String sql = "SELECT * FROM loans ORDER BY applied_at DESC";
        List<LoanApplication> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToLoan(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching all loans: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(pstmt, rs);
            DBConnectionManager.closeConnection(conn);
        }
    }

    @Override
    public boolean update(LoanApplication loan) throws DatabaseException {
        String sql = "UPDATE loans SET status = ?, admin_remarks = ?, reviewed_at = ?, remaining_principal = ?, emis_paid = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, loan.getStatus());
            pstmt.setString(2, loan.getRemarks());
            pstmt.setString(3, loan.getDecidedAt());
            pstmt.setDouble(4, loan.getRemainingPrincipal());
            pstmt.setInt(5, loan.getEmisPaid());
            pstmt.setString(6, loan.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating loan: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(conn, pstmt);
        }
    }

    @Override
    public boolean updateStatus(String loanId, String newStatus, String remarks) throws DatabaseException {
        String sql = "UPDATE loans SET status = ?, admin_remarks = ?, reviewed_at = CURRENT_TIMESTAMP WHERE id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, newStatus);
            pstmt.setString(2, remarks);
            pstmt.setString(3, loanId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating loan status: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(conn, pstmt);
        }
    }

    @Override
    public boolean deleteById(String id) throws DatabaseException {
        String sql = "DELETE FROM loans WHERE id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting loan: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(conn, pstmt);
        }
    }

    private LoanApplication mapResultSetToLoan(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String customerId = rs.getString("customer_id");
        String applicantName = rs.getString("applicant_name");
        String loanType = rs.getString("loan_type");
        double amount = rs.getDouble("amount");
        int tenure = rs.getInt("tenure_months");
        double interest = rs.getDouble("interest_rate");
        String purpose = rs.getString("purpose");

        LoanApplication loan = new LoanApplication(id, customerId, applicantName, loanType, amount, tenure, interest, purpose);
        loan.setStatus(rs.getString("status"));
        loan.setRemarks(rs.getString("admin_remarks"));
        loan.setAppliedAt(rs.getString("applied_at"));
        loan.setDecidedAt(rs.getString("reviewed_at"));
        loan.setRemainingPrincipal(rs.getDouble("remaining_principal"));
        loan.setEmisPaid(rs.getInt("emis_paid"));
        return loan;
    }
}
