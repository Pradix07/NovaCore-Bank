package com.bank.repository;

import com.bank.dao.*;
import com.bank.dao.impl.*;
import com.bank.model.*;
import com.bank.util.SecurityUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * ============================================================================
 * DATA STORE (Thread-Safe Persistence & Repository backed by SQLite JDBC)
 * ============================================================================
 * Coordinates with DAO layer to provide ACID persistence to SQLite JDBC database.
 * Supports Users, Accounts, Transactions, Loans, Investments, Audit Logs, Settings.
 */
public class DataStore {

    private static DataStore instance;

    // DAO Layer
    private final UserDAO userDAO = new UserDAOImpl();
    private final AccountDAO accountDAO = new AccountDAOImpl();
    private final TransactionDAO transactionDAO = new TransactionDAOImpl();
    private final LoanDAO loanDAO = new LoanDAOImpl();
    private final AuditLogDAO auditLogDAO = new AuditLogDAOImpl();
    private final DBConnectionManager dbManager = DBConnectionManager.getInstance();

    // In-memory cache for fast lookups & thread safety
    private final Map<String, User> usersById = new ConcurrentHashMap<>();
    private final Map<String, User> usersByUsername = new ConcurrentHashMap<>();
    private final Map<String, Account> accountsByNumber = new ConcurrentHashMap<>();
    private final List<Transaction> transactions = new CopyOnWriteArrayList<>();
    private final List<LoanApplication> loans = new CopyOnWriteArrayList<>();
    private final List<Investment> investments = new CopyOnWriteArrayList<>();
    private final List<AuditLog> auditLogs = new CopyOnWriteArrayList<>();
    private final Map<String, String> sessionTokenToUserId = new ConcurrentHashMap<>();
    private SystemSettings systemSettings = new SystemSettings();

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private DataStore() {
        initStorage();
    }

    public static synchronized DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    private void initStorage() {
        try {
            // Initialize SQLite schema and tables
            DatabaseInitializer.initializeDatabase();
            loadFromDatabase();
        } catch (Exception e) {
            System.err.println("[DataStore] Warning during database initialization: " + e.getMessage());
            e.printStackTrace();
            seedInitialData();
        }
    }

    public synchronized void loadFromDatabase() {
        lock.writeLock().lock();
        try {
            usersById.clear();
            usersByUsername.clear();
            accountsByNumber.clear();
            transactions.clear();
            loans.clear();
            investments.clear();
            auditLogs.clear();

            List<User> dbUsers = userDAO.findAll();
            if (dbUsers == null || dbUsers.isEmpty()) {
                System.out.println("[DataStore] No existing records found in SQLite database. Seeding initial data...");
                seedInitialData();
                return;
            }

            System.out.println("[DataStore] Loading existing banking records from SQLite database...");

            // 1. Load Users
            for (User u : dbUsers) {
                usersById.put(u.getId(), u);
                usersByUsername.put(u.getUsername().toLowerCase(), u);
            }

            // 2. Load Accounts
            List<Account> dbAccounts = accountDAO.findAll();
            for (Account a : dbAccounts) {
                accountsByNumber.put(a.getAccountNumber(), a);
                User u = usersById.get(a.getCustomerId());
                if (u instanceof Customer) {
                    ((Customer) u).addAccountNumber(a.getAccountNumber());
                }
            }

            // 3. Load Transactions
            List<Transaction> dbTx = transactionDAO.findAll();
            transactions.addAll(dbTx);

            // 4. Load Loans
            List<LoanApplication> dbLoans = loanDAO.findAll();
            loans.addAll(dbLoans);

            // 5. Load Audit Logs
            List<AuditLog> dbLogs = auditLogDAO.findAll();
            auditLogs.addAll(dbLogs);

            // 6. Load Investments
            loadInvestmentsFromDB();

            // 7. Load Settings
            loadSettingsFromDB();

            System.out.println("[DataStore] Successfully loaded: " + usersById.size() + " users, "
                    + accountsByNumber.size() + " accounts, " + transactions.size() + " transactions, "
                    + loans.size() + " loans, " + investments.size() + " investments, "
                    + auditLogs.size() + " audit logs.");

        } catch (Exception e) {
            System.err.println("[DataStore] Error loading from SQLite: " + e.getMessage());
            e.printStackTrace();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void seedInitialData() {
        usersById.clear();
        usersByUsername.clear();
        accountsByNumber.clear();
        transactions.clear();
        loans.clear();
        investments.clear();
        auditLogs.clear();

        // 1. Admin User - Chief Branch Manager
        Admin admin = new Admin(
                "USR-ADMIN-01",
                "admin",
                SecurityUtil.hashPassword("admin123"),
                "Rajesh Kumar Sharma",
                "admin@novacorebank.in",
                "+91-98100-11223",
                "Banking Operations & Compliance",
                5
        );
        addUser(admin);

        // 2. Customer 1 - Aarav Patel (Mumbai)
        Customer aarav = new Customer(
                "USR-CUST-101",
                "aarav.patel",
                SecurityUtil.hashPassword("customer123"),
                "Aarav Patel",
                "aarav.patel@example.com",
                "+91-98765-43210",
                "402, Marine Drive, Nariman Point, Mumbai, Maharashtra 400021",
                "AAAPA1234B",
                "XXXX-XXXX-9876",
                "aarav@novabank",
                "1234"
        );
        aarav.setOccupation("Principal Cloud Architect");
        aarav.setMonthlyIncome(185000.0);

        SavingsAccount aaravSavings = new SavingsAccount("10010001001", aarav.getId(), 74500.00, "INR", 0.040, 1000.0, 50000.0);
        aaravSavings.setIfscCode("NOVA0001001");
        aaravSavings.setBranchName("Mumbai Fort Main Branch");
        aaravSavings.setUpiId("aarav@novabank");
        aaravSavings.setCardNumber("6080123456789012");
        aaravSavings.setCardExpiry("10/29");
        aaravSavings.setCardCvv("742");

        CurrentAccount aaravCurrent = new CurrentAccount(
                "20010002002",
                aarav.getId(),
                150000.00,
                "INR",
                "27AAAPA1234B1Z5",
                "Apex Tech Solutions LLP",
                100000.0,
                5000.0
        );
        aaravCurrent.setIfscCode("NOVA0001001");
        aaravCurrent.setBranchName("Mumbai Fort Main Branch");
        aaravCurrent.setUpiId("apextech@novabank");
        aaravCurrent.setCardNumber("6080987654321098");
        aaravCurrent.setCardExpiry("05/30");
        aaravCurrent.setCardCvv("318");

        aarav.addAccountNumber(aaravSavings.getAccountNumber());
        aarav.addAccountNumber(aaravCurrent.getAccountNumber());

        addUser(aarav);
        addAccount(aaravSavings);
        addAccount(aaravCurrent);

        // Also add legacy alias user 'alex.morgan' pointing to Aarav's credentials for demo convenience
        Customer alexAlias = new Customer(
                "USR-CUST-101-ALIAS",
                "alex.morgan",
                SecurityUtil.hashPassword("customer123"),
                "Aarav Patel (Demo)",
                "alex.morgan@example.com",
                "+91-98765-43210",
                "402, Marine Drive, Nariman Point, Mumbai, Maharashtra 400021",
                "AAAPA1234B",
                "XXXX-XXXX-9876",
                "aarav@novabank",
                "1234"
        );
        alexAlias.setOccupation("Principal Cloud Architect");
        alexAlias.setMonthlyIncome(185000.0);
        alexAlias.addAccountNumber(aaravSavings.getAccountNumber());
        alexAlias.addAccountNumber(aaravCurrent.getAccountNumber());
        addUser(alexAlias);

        // 3. Customer 2 - Priya Sharma (Bengaluru)
        Customer priya = new Customer(
                "USR-CUST-102",
                "priya.sharma",
                SecurityUtil.hashPassword("customer123"),
                "Priya Sharma",
                "priya.sharma@example.com",
                "+91-98123-45678",
                "88, Indiranagar 100ft Road, Bengaluru, Karnataka 560038",
                "BBBPB5678C",
                "XXXX-XXXX-5432",
                "priya@novabank",
                "4321"
        );
        priya.setOccupation("Senior Financial Analyst");
        priya.setMonthlyIncome(145000.0);

        SavingsAccount priyaSavings = new SavingsAccount("10010003003", priya.getId(), 58250.00, "INR", 0.040, 1000.0, 50000.0);
        priyaSavings.setIfscCode("NOVA0002002");
        priyaSavings.setBranchName("Bengaluru Electronic City Branch");
        priyaSavings.setUpiId("priya@novabank");
        priyaSavings.setCardNumber("6080334455667788");
        priyaSavings.setCardExpiry("08/28");
        priyaSavings.setCardCvv("905");
        priya.addAccountNumber(priyaSavings.getAccountNumber());

        addUser(priya);
        addAccount(priyaSavings);

        // 4. Customer 3 - Rohan Verma (Student at IIT Delhi)
        Customer rohan = new Customer(
                "USR-CUST-103",
                "rohan.verma",
                SecurityUtil.hashPassword("customer123"),
                "Rohan Verma",
                "rohan.verma@example.com",
                "+91-99887-76655",
                "Hostel 12, IIT Delhi Campus, Hauz Khas, New Delhi 110016",
                "CCCPC9012D",
                "XXXX-XXXX-1122",
                "rohan@novabank",
                "1122"
        );
        rohan.setOccupation("Student (B.Tech Computer Science)");
        rohan.setMonthlyIncome(15000.0);

        StudentAccount rohanStudent = new StudentAccount(
                "30010004004",
                rohan.getId(),
                18450.00,
                "INR",
                "Indian Institute of Technology (IIT) Delhi",
                "IITD-2024-CS42",
                0.035,
                100.0,
                100000.0,
                20000.0
        );
        rohanStudent.setIfscCode("NOVA0003003");
        rohanStudent.setBranchName("New Delhi Connaught Place Branch");
        rohanStudent.setUpiId("rohan@novabank");
        rohanStudent.setCardNumber("6080998877665544");
        rohanStudent.setCardExpiry("11/29");
        rohanStudent.setCardCvv("442");
        rohan.addAccountNumber(rohanStudent.getAccountNumber());

        addUser(rohan);
        addAccount(rohanStudent);

        // Sample Indian Banking Transactions
        addTransaction(new Transaction("TXN-IN-001", TransactionType.DEPOSIT, 50000.00, "SALARY_CREDIT_IMPS", aaravSavings.getAccountNumber(), "Monthly Salary Credit via IMPS", 74500.00, aarav.getId(), "IMPS92810394812"));
        addTransaction(new Transaction("TXN-IN-002", TransactionType.TRANSFER_OUT, 15000.00, aaravSavings.getAccountNumber(), priyaSavings.getAccountNumber(), "UPI Transfer to Priya Sharma (NOVA0002002)", 59500.00, aarav.getId(), "UPI202610078891"));
        addTransaction(new Transaction("TXN-IN-003", TransactionType.TRANSFER_IN, 15000.00, aaravSavings.getAccountNumber(), priyaSavings.getAccountNumber(), "UPI Payment Received from Aarav Patel", 58250.00, priya.getId(), "UPI202610078891"));
        addTransaction(new Transaction("TXN-IN-004", TransactionType.WITHDRAWAL, 5000.00, aaravCurrent.getAccountNumber(), "ATM_MUMBAI_FORT", "RuPay ATM Cash Withdrawal", 150000.00, aarav.getId(), "ATM20269988112"));
        addTransaction(new Transaction("TXN-IN-005", TransactionType.DEPOSIT, 10000.00, "SCHOLARSHIP_NEFT", rohanStudent.getAccountNumber(), "National Merit Scholarship NEFT Disbursal", 18450.00, rohan.getId(), "NEFT9081237461"));

        // Sample Loans (INR)
        LoanApplication loan1 = new LoanApplication("LOAN-IN-701", aarav.getId(), aarav.getFullName(), "HOME", 4500000.0, 180, 8.4, "Apartment Purchase in Mumbai Suburban");
        loan1.setStatus("APPROVED");
        loan1.setDecidedAt("2026-09-15 10:30:00");
        loan1.setEmisPaid(6);
        loan1.setRemainingPrincipal(4420000.0);
        addLoan(loan1);

        LoanApplication loan2 = new LoanApplication("LOAN-IN-702", priya.getId(), priya.getFullName(), "EDUCATION", 800000.0, 48, 7.2, "Post Graduate Financial Analytics Degree");
        loan2.setStatus("PENDING");
        addLoan(loan2);

        // Sample Indian Investments
        addInvestment(new Investment("INV-IN-901", aarav.getId(), "FIXED_DEPOSIT", "Nova Shrestha 1-Year FD (7.5% p.a.)", 100000.0, 7.5, 12));
        addInvestment(new Investment("INV-IN-902", aarav.getId(), "MUTUAL_FUND", "Nifty 50 Bluechip Index Growth Fund", 50000.0, 12.5, 24));
        addInvestment(new Investment("INV-IN-903", priya.getId(), "GOLD_BOND", "RBI Sovereign Gold Bond (SGB) Series 2026", 75000.0, 6.5, 36));

        // Audit Logs
        addAuditLog(new AuditLog("LOG-IN-001", "USR-ADMIN-01", "Rajesh Kumar Sharma", "ADMIN", "SYSTEM_INIT", "NovaCore Bank of India core platform initialized with IFSC NOVA0001001.", "127.0.0.1"));
        addAuditLog(new AuditLog("LOG-IN-002", "USR-ADMIN-01", "Rajesh Kumar Sharma", "ADMIN", "LOAN_APPROVAL", "Approved Home Loan LOAN-IN-701 for customer Aarav Patel (₹45,00,000.00).", "127.0.0.1"));

        // Save System Settings
        saveSettingsToDB(systemSettings);
    }

    /**
     * Persists all data. As operations are directly backed by SQLite JDBC, this method
     * serves to flush any pending states and ensure consistency.
     */
    public synchronized void saveToFile() {
        // SQLite JDBC persistence is immediate and transactional.
    }

    // ============================================================================
    // USER OPERATIONS (Backed by UserDAO)
    // ============================================================================
    public void addUser(User user) {
        if (user == null) return;
        usersById.put(user.getId(), user);
        usersByUsername.put(user.getUsername().toLowerCase(), user);
        try {
            if (userDAO.findById(user.getId()).isPresent()) {
                userDAO.update(user);
            } else {
                userDAO.save(user);
            }
        } catch (Exception e) {
            System.err.println("[DataStore] Failed to persist user to SQLite: " + e.getMessage());
        }
    }

    public void updateUser(User user) {
        if (user == null) return;
        usersById.put(user.getId(), user);
        usersByUsername.put(user.getUsername().toLowerCase(), user);
        try {
            userDAO.update(user);
        } catch (Exception e) {
            System.err.println("[DataStore] Failed to update user in SQLite: " + e.getMessage());
        }
    }

    public void removeUser(String userId) {
        User user = usersById.remove(userId);
        if (user != null) {
            usersByUsername.remove(user.getUsername().toLowerCase());
        }
        try {
            userDAO.deleteById(userId);
        } catch (Exception e) {
            System.err.println("[DataStore] Failed to delete user from SQLite: " + e.getMessage());
        }
    }

    public User getUserById(String id) {
        if (id == null) return null;
        User cached = usersById.get(id);
        if (cached != null) return cached;
        try {
            Optional<User> u = userDAO.findById(id);
            if (u.isPresent()) {
                usersById.put(u.get().getId(), u.get());
                usersByUsername.put(u.get().getUsername().toLowerCase(), u.get());
                return u.get();
            }
        } catch (Exception ignored) {}
        return null;
    }

    public User getUserByUsername(String username) {
        if (username == null) return null;
        User cached = usersByUsername.get(username.toLowerCase());
        if (cached != null) return cached;
        try {
            Optional<User> u = userDAO.findByUsername(username);
            if (u.isPresent()) {
                usersById.put(u.get().getId(), u.get());
                usersByUsername.put(u.get().getUsername().toLowerCase(), u.get());
                return u.get();
            }
        } catch (Exception ignored) {}
        return null;
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(usersById.values());
    }

    // ============================================================================
    // ACCOUNT OPERATIONS (Backed by AccountDAO)
    // ============================================================================
    public void addAccount(Account account) {
        if (account == null) return;
        accountsByNumber.put(account.getAccountNumber(), account);
        User u = usersById.get(account.getCustomerId());
        if (u instanceof Customer) {
            ((Customer) u).addAccountNumber(account.getAccountNumber());
        }
        try {
            if (accountDAO.findById(account.getAccountNumber()).isPresent()) {
                accountDAO.update(account);
            } else {
                accountDAO.save(account);
            }
        } catch (Exception e) {
            System.err.println("[DataStore] Failed to persist account to SQLite: " + e.getMessage());
        }
    }

    public void updateAccount(Account account) {
        if (account == null) return;
        accountsByNumber.put(account.getAccountNumber(), account);
        try {
            accountDAO.update(account);
        } catch (Exception e) {
            System.err.println("[DataStore] Failed to update account in SQLite: " + e.getMessage());
        }
    }

    public void removeAccount(String accountNumber) {
        accountsByNumber.remove(accountNumber);
        try {
            accountDAO.deleteById(accountNumber);
        } catch (Exception e) {
            System.err.println("[DataStore] Failed to delete account from SQLite: " + e.getMessage());
        }
    }

    public Account getAccountByNumber(String accountNumber) {
        if (accountNumber == null) return null;
        Account cached = accountsByNumber.get(accountNumber.trim());
        if (cached != null) return cached;
        try {
            Optional<Account> acc = accountDAO.findById(accountNumber.trim());
            if (acc.isPresent()) {
                accountsByNumber.put(acc.get().getAccountNumber(), acc.get());
                return acc.get();
            }
        } catch (Exception ignored) {}
        return null;
    }

    public List<Account> getAccountsByCustomerId(String customerId) {
        List<Account> res = new ArrayList<>();
        for (Account a : accountsByNumber.values()) {
            if (a.getCustomerId().equals(customerId)) {
                res.add(a);
            }
        }
        return res;
    }

    public List<Account> getAllAccounts() {
        return new ArrayList<>(accountsByNumber.values());
    }

    // ============================================================================
    // TRANSACTION OPERATIONS (Backed by TransactionDAO)
    // ============================================================================
    public void addTransaction(Transaction transaction) {
        if (transaction == null) return;
        transactions.add(0, transaction); // most recent first
        try {
            transactionDAO.save(transaction);
        } catch (Exception e) {
            System.err.println("[DataStore] Failed to persist transaction to SQLite: " + e.getMessage());
        }
    }

    public List<Transaction> getAllTransactions() {
        return new ArrayList<>(transactions);
    }

    public List<Transaction> getTransactionsByCustomerId(String customerId) {
        List<Transaction> res = new ArrayList<>();
        for (Transaction t : transactions) {
            if (customerId != null && customerId.equals(t.getCustomerId())) {
                res.add(t);
            }
        }
        return res;
    }

    // ============================================================================
    // LOAN OPERATIONS (Backed by LoanDAO)
    // ============================================================================
    public void addLoan(LoanApplication loan) {
        if (loan == null) return;
        loans.add(0, loan);
        try {
            loanDAO.save(loan);
        } catch (Exception e) {
            System.err.println("[DataStore] Failed to persist loan to SQLite: " + e.getMessage());
        }
    }

    public void updateLoan(LoanApplication loan) {
        if (loan == null) return;
        for (int i = 0; i < loans.size(); i++) {
            if (loans.get(i).getId().equals(loan.getId())) {
                loans.set(i, loan);
                break;
            }
        }
        try {
            loanDAO.update(loan);
        } catch (Exception e) {
            System.err.println("[DataStore] Failed to update loan in SQLite: " + e.getMessage());
        }
    }

    public List<LoanApplication> getAllLoans() {
        return new ArrayList<>(loans);
    }

    public LoanApplication getLoanById(String id) {
        if (id == null) return null;
        for (LoanApplication l : loans) {
            if (l.getId().equals(id)) return l;
        }
        try {
            Optional<LoanApplication> loan = loanDAO.findById(id);
            if (loan.isPresent()) return loan.get();
        } catch (Exception ignored) {}
        return null;
    }

    public List<LoanApplication> getLoansByCustomerId(String customerId) {
        List<LoanApplication> res = new ArrayList<>();
        for (LoanApplication l : loans) {
            if (customerId != null && customerId.equals(l.getCustomerId())) {
                res.add(l);
            }
        }
        return res;
    }

    // ============================================================================
    // INVESTMENT OPERATIONS (Backed by SQLite JDBC)
    // ============================================================================
    public void addInvestment(Investment investment) {
        if (investment == null) return;
        investments.add(0, investment);
        saveInvestmentToDB(investment);
    }

    public void updateInvestment(Investment investment) {
        if (investment == null) return;
        for (int i = 0; i < investments.size(); i++) {
            if (investments.get(i).getId().equals(investment.getId())) {
                investments.set(i, investment);
                break;
            }
        }
        updateInvestmentInDB(investment);
    }

    public List<Investment> getAllInvestments() {
        return new ArrayList<>(investments);
    }

    public Investment getInvestmentById(String id) {
        if (id == null) return null;
        for (Investment inv : investments) {
            if (inv.getId().equals(id)) return inv;
        }
        return null;
    }

    public List<Investment> getInvestmentsByCustomerId(String customerId) {
        List<Investment> res = new ArrayList<>();
        for (Investment inv : investments) {
            if (customerId != null && customerId.equals(inv.getCustomerId())) {
                res.add(inv);
            }
        }
        return res;
    }

    private void saveInvestmentToDB(Investment inv) {
        String sql = "INSERT INTO investments (id, customer_id, type, name, principal_amount, interest_rate, "
                + "duration_months, expected_return, current_maturity_value, start_date, maturity_date, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, inv.getId());
            pstmt.setString(2, inv.getCustomerId());
            pstmt.setString(3, inv.getType());
            pstmt.setString(4, inv.getName());
            pstmt.setDouble(5, inv.getPrincipalAmount());
            pstmt.setDouble(6, inv.getInterestRate());
            pstmt.setInt(7, inv.getDurationMonths());
            pstmt.setDouble(8, inv.getExpectedReturn());
            pstmt.setDouble(9, inv.getCurrentMaturityValue());
            pstmt.setString(10, inv.getStartDate());
            pstmt.setString(11, inv.getMaturityDate());
            pstmt.setString(12, inv.getStatus());
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("[DataStore] Failed to save investment to SQLite: " + e.getMessage());
        } finally {
            DBConnectionManager.closeResources(conn, pstmt);
        }
    }

    private void updateInvestmentInDB(Investment inv) {
        String sql = "UPDATE investments SET status = ?, current_maturity_value = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, inv.getStatus());
            pstmt.setDouble(2, inv.getCurrentMaturityValue());
            pstmt.setString(3, inv.getId());
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("[DataStore] Failed to update investment in SQLite: " + e.getMessage());
        } finally {
            DBConnectionManager.closeResources(conn, pstmt);
        }
    }

    private void loadInvestmentsFromDB() {
        String sql = "SELECT * FROM investments ORDER BY start_date DESC";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Investment inv = new Investment();
                inv.setId(rs.getString("id"));
                inv.setCustomerId(rs.getString("customer_id"));
                inv.setType(rs.getString("type"));
                inv.setName(rs.getString("name"));
                inv.setPrincipalAmount(rs.getDouble("principal_amount"));
                inv.setInterestRate(rs.getDouble("interest_rate"));
                inv.setDurationMonths(rs.getInt("duration_months"));
                inv.setExpectedReturn(rs.getDouble("expected_return"));
                inv.setCurrentMaturityValue(rs.getDouble("current_maturity_value"));
                inv.setStartDate(rs.getString("start_date"));
                inv.setMaturityDate(rs.getString("maturity_date"));
                inv.setStatus(rs.getString("status"));
                investments.add(inv);
            }
        } catch (Exception e) {
            System.err.println("[DataStore] Failed to load investments from SQLite: " + e.getMessage());
        } finally {
            DBConnectionManager.closeResources(pstmt, rs);
            DBConnectionManager.closeConnection(conn);
        }
    }

    // ============================================================================
    // AUDIT LOG OPERATIONS (Backed by AuditLogDAO)
    // ============================================================================
    public void addAuditLog(AuditLog log) {
        if (log == null) return;
        auditLogs.add(0, log);
        try {
            auditLogDAO.save(log);
        } catch (Exception e) {
            System.err.println("[DataStore] Failed to persist audit log to SQLite: " + e.getMessage());
        }
    }

    public List<AuditLog> getAllAuditLogs() {
        return new ArrayList<>(auditLogs);
    }

    // ============================================================================
    // SYSTEM SETTINGS (Backed by SQLite JDBC)
    // ============================================================================
    public SystemSettings getSystemSettings() {
        return systemSettings;
    }

    public void setSystemSettings(SystemSettings settings) {
        if (settings == null) return;
        this.systemSettings = settings;
        saveSettingsToDB(settings);
    }

    private void saveSettingsToDB(SystemSettings s) {
        String sql = "INSERT OR REPLACE INTO system_settings (id, bank_name, default_savings_interest_rate, "
                + "default_loan_interest_rate, default_fd_interest_rate, daily_transfer_limit, per_transaction_limit, "
                + "min_savings_balance, transaction_fee_percent, maintenance_mode, support_email, support_phone, currency_symbol) "
                + "VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, s.getBankName());
            pstmt.setDouble(2, s.getDefaultSavingsInterestRate());
            pstmt.setDouble(3, s.getDefaultLoanInterestRate());
            pstmt.setDouble(4, s.getDefaultFdInterestRate());
            pstmt.setDouble(5, s.getDailyTransferLimit());
            pstmt.setDouble(6, s.getPerTransactionLimit());
            pstmt.setDouble(7, s.getMinSavingsBalance());
            pstmt.setDouble(8, s.getTransactionFeePercent());
            pstmt.setBoolean(9, s.isMaintenanceMode());
            pstmt.setString(10, s.getSupportEmail());
            pstmt.setString(11, s.getSupportPhone());
            pstmt.setString(12, s.getCurrencySymbol());
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("[DataStore] Failed to save settings to SQLite: " + e.getMessage());
        } finally {
            DBConnectionManager.closeResources(conn, pstmt);
        }
    }

    private void loadSettingsFromDB() {
        String sql = "SELECT * FROM system_settings WHERE id = 1";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dbManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                systemSettings.setBankName(rs.getString("bank_name"));
                systemSettings.setDefaultSavingsInterestRate(rs.getDouble("default_savings_interest_rate"));
                systemSettings.setDefaultLoanInterestRate(rs.getDouble("default_loan_interest_rate"));
                systemSettings.setDefaultFdInterestRate(rs.getDouble("default_fd_interest_rate"));
                systemSettings.setDailyTransferLimit(rs.getDouble("daily_transfer_limit"));
                systemSettings.setPerTransactionLimit(rs.getDouble("per_transaction_limit"));
                systemSettings.setMinSavingsBalance(rs.getDouble("min_savings_balance"));
                systemSettings.setTransactionFeePercent(rs.getDouble("transaction_fee_percent"));
                systemSettings.setMaintenanceMode(rs.getBoolean("maintenance_mode"));
                systemSettings.setSupportEmail(rs.getString("support_email"));
                systemSettings.setSupportPhone(rs.getString("support_phone"));
                systemSettings.setCurrencySymbol(rs.getString("currency_symbol"));
            } else {
                saveSettingsToDB(systemSettings);
            }
        } catch (Exception e) {
            System.err.println("[DataStore] Failed to load settings from SQLite: " + e.getMessage());
        } finally {
            DBConnectionManager.closeResources(pstmt, rs);
            DBConnectionManager.closeConnection(conn);
        }
    }

    // ============================================================================
    // SESSIONS
    // ============================================================================
    public void createSession(String token, String userId) {
        sessionTokenToUserId.put(token, userId);
    }

    public String getUserIdByToken(String token) {
        if (token == null) return null;
        return sessionTokenToUserId.get(token);
    }

    public void removeSession(String token) {
        if (token != null) {
            sessionTokenToUserId.remove(token);
        }
    }
}
