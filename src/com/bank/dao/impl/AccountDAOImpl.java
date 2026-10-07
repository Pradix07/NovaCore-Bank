package com.bank.dao.impl;

import com.bank.dao.AccountDAO;
import com.bank.dao.DBConnectionManager;
import com.bank.exceptions.DatabaseException;
import com.bank.model.Account;
import com.bank.model.CheckingAccount;
import com.bank.model.CurrentAccount;
import com.bank.model.SavingsAccount;
import com.bank.model.StudentAccount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * CLASS: AccountDAOImpl (JDBC Implementation)
 * ============================================================================
 * Demonstrates:
 * - Interface Implementation (implements AccountDAO)
 * - Polymorphism: Handles SavingsAccount, CurrentAccount, StudentAccount, CheckingAccount
 * - Safe parameterized queries and ACID compliance
 */
public class AccountDAOImpl implements AccountDAO {

    private final DBConnectionManager dbManager = DBConnectionManager.getInstance();

    @Override
    public boolean save(Account account) throws DatabaseException {
        String sql = "INSERT INTO accounts (account_number, customer_id, account_type, balance, currency, "
                + "status, interest_rate, minimum_balance, overdraft_limit, card_number, card_expiry, card_cvv, card_frozen, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, account.getAccountNumber());
            pstmt.setString(2, account.getCustomerId());
            pstmt.setString(3, account.getAccountType());
            pstmt.setDouble(4, account.getBalance());
            pstmt.setString(5, account.getCurrency());
            pstmt.setString(6, account.getStatus());

            if (account instanceof SavingsAccount) {
                SavingsAccount sa = (SavingsAccount) account;
                pstmt.setDouble(7, sa.getInterestRate());
                pstmt.setDouble(8, sa.getMinimumBalance());
                pstmt.setDouble(9, 0.0);
            } else if (account instanceof CurrentAccount) {
                CurrentAccount ca = (CurrentAccount) account;
                pstmt.setDouble(7, 0.0);
                pstmt.setDouble(8, ca.getMinimumBalance());
                pstmt.setDouble(9, ca.getOverdraftLimit());
            } else if (account instanceof StudentAccount) {
                StudentAccount sta = (StudentAccount) account;
                pstmt.setDouble(7, sta.getInterestRate());
                pstmt.setDouble(8, sta.getMinimumBalance());
                pstmt.setDouble(9, 0.0);
            } else if (account instanceof CheckingAccount) {
                CheckingAccount ca = (CheckingAccount) account;
                pstmt.setDouble(7, 0.0);
                pstmt.setDouble(8, 0.0);
                pstmt.setDouble(9, ca.getOverdraftLimit());
            } else {
                pstmt.setDouble(7, 0.0);
                pstmt.setDouble(8, 0.0);
                pstmt.setDouble(9, 0.0);
            }

            pstmt.setString(10, account.getCardNumber());
            pstmt.setString(11, account.getCardExpiry());
            pstmt.setString(12, account.getCardCvv());
            pstmt.setBoolean(13, account.isCardFrozen());
            pstmt.setString(14, account.getCreatedAt());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException("Error inserting account into database: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(conn, pstmt);
        }
    }

    @Override
    public Optional<Account> findById(String accountNumber) throws DatabaseException {
        String sql = "SELECT * FROM accounts WHERE account_number = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, accountNumber);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToAccount(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Error finding account by number: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(pstmt, rs);
            DBConnectionManager.closeConnection(conn);
        }
    }

    @Override
    public List<Account> findByCustomerId(String customerId) throws DatabaseException {
        String sql = "SELECT * FROM accounts WHERE customer_id = ?";
        List<Account> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, customerId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToAccount(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error finding accounts by customer ID: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(pstmt, rs);
            DBConnectionManager.closeConnection(conn);
        }
    }

    @Override
    public List<Account> findAll() throws DatabaseException {
        String sql = "SELECT * FROM accounts";
        List<Account> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToAccount(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching all accounts: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(pstmt, rs);
            DBConnectionManager.closeConnection(conn);
        }
    }

    @Override
    public boolean update(Account account) throws DatabaseException {
        String sql = "UPDATE accounts SET balance = ?, status = ?, card_number = ?, card_expiry = ?, card_cvv = ?, card_frozen = ? WHERE account_number = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setDouble(1, account.getBalance());
            pstmt.setString(2, account.getStatus());
            pstmt.setString(3, account.getCardNumber());
            pstmt.setString(4, account.getCardExpiry());
            pstmt.setString(5, account.getCardCvv());
            pstmt.setBoolean(6, account.isCardFrozen());
            pstmt.setString(7, account.getAccountNumber());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating account: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(conn, pstmt);
        }
    }

    @Override
    public boolean updateBalance(String accountNumber, double newBalance) throws DatabaseException {
        String sql = "UPDATE accounts SET balance = ? WHERE account_number = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setDouble(1, newBalance);
            pstmt.setString(2, accountNumber);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating balance: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(conn, pstmt);
        }
    }

    @Override
    public boolean updateCardStatus(String accountNumber, boolean isFrozen) throws DatabaseException {
        String sql = "UPDATE accounts SET card_frozen = ? WHERE account_number = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setBoolean(1, isFrozen);
            pstmt.setString(2, accountNumber);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating card status: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(conn, pstmt);
        }
    }

    @Override
    public boolean deleteById(String accountNumber) throws DatabaseException {
        String sql = "DELETE FROM accounts WHERE account_number = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, accountNumber);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting account: " + e.getMessage(), e);
        } finally {
            DBConnectionManager.closeResources(conn, pstmt);
        }
    }

    private Account mapResultSetToAccount(ResultSet rs) throws SQLException {
        String type = rs.getString("account_type");
        String accNum = rs.getString("account_number");
        String custId = rs.getString("customer_id");
        double balance = rs.getDouble("balance");
        String currency = rs.getString("currency");
        String status = rs.getString("status");

        Account account;
        if ("SAVINGS".equalsIgnoreCase(type)) {
            double rate = rs.getDouble("interest_rate");
            double minBal = rs.getDouble("minimum_balance");
            account = new SavingsAccount(accNum, custId, balance, currency, rate, minBal);
        } else if ("CURRENT".equalsIgnoreCase(type)) {
            double overdraft = rs.getDouble("overdraft_limit");
            double minBal = rs.getDouble("minimum_balance");
            account = new CurrentAccount(accNum, custId, balance, currency, "GSTIN-VERIFIED", "Commercial Enterprise", overdraft, minBal);
        } else if ("STUDENT".equalsIgnoreCase(type)) {
            double rate = rs.getDouble("interest_rate");
            double minBal = rs.getDouble("minimum_balance");
            account = new StudentAccount(accNum, custId, balance, currency, "Educational Institution", "STU-0000", rate, minBal, 100000.0, 20000.0);
        } else {
            double overdraft = rs.getDouble("overdraft_limit");
            account = new CheckingAccount(accNum, custId, balance, currency, overdraft);
        }

        account.setStatus(status);
        account.setCardNumber(rs.getString("card_number"));
        account.setCardExpiry(rs.getString("card_expiry"));
        account.setCardCvv(rs.getString("card_cvv"));
        account.setCardFrozen(rs.getBoolean("card_frozen"));
        account.setCreatedAt(rs.getString("created_at"));
        return account;
    }
}
