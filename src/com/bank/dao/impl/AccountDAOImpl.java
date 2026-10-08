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
                + "status, interest_rate, minimum_balance, overdraft_limit, max_withdraw_limit, max_balance_limit, "
                + "ifsc_code, branch_name, upi_id, nominee_name, trade_license_or_gst, business_name, "
                + "institution_name, student_id, card_number, card_expiry, card_cvv, card_frozen, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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
                pstmt.setDouble(10, sa.getMaxWithdrawLimit());
                pstmt.setDouble(11, 0.0);
                pstmt.setString(16, null);
                pstmt.setString(17, null);
                pstmt.setString(18, null);
                pstmt.setString(19, null);
            } else if (account instanceof CurrentAccount) {
                CurrentAccount ca = (CurrentAccount) account;
                pstmt.setDouble(7, 0.0);
                pstmt.setDouble(8, ca.getMinimumBalance());
                pstmt.setDouble(9, ca.getOverdraftLimit());
                pstmt.setDouble(10, 0.0);
                pstmt.setDouble(11, 0.0);
                pstmt.setString(16, ca.getTradeLicenseOrGst());
                pstmt.setString(17, ca.getBusinessName());
                pstmt.setString(18, null);
                pstmt.setString(19, null);
            } else if (account instanceof StudentAccount) {
                StudentAccount sta = (StudentAccount) account;
                pstmt.setDouble(7, sta.getInterestRate());
                pstmt.setDouble(8, sta.getMinimumBalance());
                pstmt.setDouble(9, 0.0);
                pstmt.setDouble(10, sta.getMaxWithdrawLimit());
                pstmt.setDouble(11, sta.getMaxBalanceLimit());
                pstmt.setString(16, null);
                pstmt.setString(17, null);
                pstmt.setString(18, sta.getInstitutionName());
                pstmt.setString(19, sta.getStudentId());
            } else if (account instanceof CheckingAccount) {
                CheckingAccount ca = (CheckingAccount) account;
                pstmt.setDouble(7, 0.0);
                pstmt.setDouble(8, 0.0);
                pstmt.setDouble(9, ca.getOverdraftLimit());
                pstmt.setDouble(10, 0.0);
                pstmt.setDouble(11, 0.0);
                pstmt.setString(16, null);
                pstmt.setString(17, null);
                pstmt.setString(18, null);
                pstmt.setString(19, null);
            } else {
                pstmt.setDouble(7, 0.0);
                pstmt.setDouble(8, 0.0);
                pstmt.setDouble(9, 0.0);
                pstmt.setDouble(10, 0.0);
                pstmt.setDouble(11, 0.0);
                pstmt.setString(16, null);
                pstmt.setString(17, null);
                pstmt.setString(18, null);
                pstmt.setString(19, null);
            }

            pstmt.setString(12, account.getIfscCode());
            pstmt.setString(13, account.getBranchName());
            pstmt.setString(14, account.getUpiId());
            pstmt.setString(15, account.getNomineeName());

            pstmt.setString(20, account.getCardNumber());
            pstmt.setString(21, account.getCardExpiry());
            pstmt.setString(22, account.getCardCvv());
            pstmt.setBoolean(23, account.isCardFrozen());
            pstmt.setString(24, account.getCreatedAt());

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
        String sql = "UPDATE accounts SET balance = ?, status = ?, interest_rate = ?, minimum_balance = ?, "
                + "overdraft_limit = ?, max_withdraw_limit = ?, max_balance_limit = ?, ifsc_code = ?, branch_name = ?, "
                + "upi_id = ?, nominee_name = ?, trade_license_or_gst = ?, business_name = ?, institution_name = ?, "
                + "student_id = ?, card_number = ?, card_expiry = ?, card_cvv = ?, card_frozen = ? WHERE account_number = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setDouble(1, account.getBalance());
            pstmt.setString(2, account.getStatus());

            if (account instanceof SavingsAccount) {
                SavingsAccount sa = (SavingsAccount) account;
                pstmt.setDouble(3, sa.getInterestRate());
                pstmt.setDouble(4, sa.getMinimumBalance());
                pstmt.setDouble(5, 0.0);
                pstmt.setDouble(6, sa.getMaxWithdrawLimit());
                pstmt.setDouble(7, 0.0);
                pstmt.setString(12, null);
                pstmt.setString(13, null);
                pstmt.setString(14, null);
                pstmt.setString(15, null);
            } else if (account instanceof CurrentAccount) {
                CurrentAccount ca = (CurrentAccount) account;
                pstmt.setDouble(3, 0.0);
                pstmt.setDouble(4, ca.getMinimumBalance());
                pstmt.setDouble(5, ca.getOverdraftLimit());
                pstmt.setDouble(6, 0.0);
                pstmt.setDouble(7, 0.0);
                pstmt.setString(12, ca.getTradeLicenseOrGst());
                pstmt.setString(13, ca.getBusinessName());
                pstmt.setString(14, null);
                pstmt.setString(15, null);
            } else if (account instanceof StudentAccount) {
                StudentAccount sta = (StudentAccount) account;
                pstmt.setDouble(3, sta.getInterestRate());
                pstmt.setDouble(4, sta.getMinimumBalance());
                pstmt.setDouble(5, 0.0);
                pstmt.setDouble(6, sta.getMaxWithdrawLimit());
                pstmt.setDouble(7, sta.getMaxBalanceLimit());
                pstmt.setString(12, null);
                pstmt.setString(13, null);
                pstmt.setString(14, sta.getInstitutionName());
                pstmt.setString(15, sta.getStudentId());
            } else if (account instanceof CheckingAccount) {
                CheckingAccount ca = (CheckingAccount) account;
                pstmt.setDouble(3, 0.0);
                pstmt.setDouble(4, 0.0);
                pstmt.setDouble(5, ca.getOverdraftLimit());
                pstmt.setDouble(6, 0.0);
                pstmt.setDouble(7, 0.0);
                pstmt.setString(12, null);
                pstmt.setString(13, null);
                pstmt.setString(14, null);
                pstmt.setString(15, null);
            } else {
                pstmt.setDouble(3, 0.0);
                pstmt.setDouble(4, 0.0);
                pstmt.setDouble(5, 0.0);
                pstmt.setDouble(6, 0.0);
                pstmt.setDouble(7, 0.0);
                pstmt.setString(12, null);
                pstmt.setString(13, null);
                pstmt.setString(14, null);
                pstmt.setString(15, null);
            }

            pstmt.setString(8, account.getIfscCode());
            pstmt.setString(9, account.getBranchName());
            pstmt.setString(10, account.getUpiId());
            pstmt.setString(11, account.getNomineeName());

            pstmt.setString(16, account.getCardNumber());
            pstmt.setString(17, account.getCardExpiry());
            pstmt.setString(18, account.getCardCvv());
            pstmt.setBoolean(19, account.isCardFrozen());
            pstmt.setString(20, account.getAccountNumber());
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
            double maxWith = rs.getDouble("max_withdraw_limit");
            account = new SavingsAccount(accNum, custId, balance, currency, rate > 0 ? rate : 0.040, minBal > 0 ? minBal : 1000.0, maxWith > 0 ? maxWith : 50000.0);
        } else if ("CURRENT".equalsIgnoreCase(type)) {
            String trade = rs.getString("trade_license_or_gst");
            String bName = rs.getString("business_name");
            double overdraft = rs.getDouble("overdraft_limit");
            double minBal = rs.getDouble("minimum_balance");
            account = new CurrentAccount(accNum, custId, balance, currency,
                    trade != null ? trade : "GSTIN-NOT-PROVIDED",
                    bName != null ? bName : "Commercial Enterprise",
                    overdraft > 0 ? overdraft : 50000.0,
                    minBal > 0 ? minBal : 5000.0);
        } else if ("STUDENT".equalsIgnoreCase(type)) {
            String inst = rs.getString("institution_name");
            String stuId = rs.getString("student_id");
            double rate = rs.getDouble("interest_rate");
            double minBal = rs.getDouble("minimum_balance");
            double maxBal = rs.getDouble("max_balance_limit");
            double maxWith = rs.getDouble("max_withdraw_limit");
            account = new StudentAccount(accNum, custId, balance, currency,
                    inst != null ? inst : "Educational Institution",
                    stuId != null ? stuId : "STU-0000",
                    rate > 0 ? rate : 0.035,
                    minBal > 0 ? minBal : 100.0,
                    maxBal > 0 ? maxBal : 100000.0,
                    maxWith > 0 ? maxWith : 20000.0);
        } else {
            double overdraft = rs.getDouble("overdraft_limit");
            account = new CheckingAccount(accNum, custId, balance, currency, overdraft > 0 ? overdraft : 25000.0);
        }

        account.setStatus(status != null ? status : "ACTIVE");
        String ifsc = rs.getString("ifsc_code");
        if (ifsc != null) account.setIfscCode(ifsc);
        String branch = rs.getString("branch_name");
        if (branch != null) account.setBranchName(branch);
        String upi = rs.getString("upi_id");
        if (upi != null) account.setUpiId(upi);
        String nominee = rs.getString("nominee_name");
        if (nominee != null) account.setNomineeName(nominee);

        account.setCardNumber(rs.getString("card_number"));
        account.setCardExpiry(rs.getString("card_expiry"));
        account.setCardCvv(rs.getString("card_cvv"));
        account.setCardFrozen(rs.getBoolean("card_frozen"));
        account.setCreatedAt(rs.getString("created_at"));
        return account;
    }
}
