package com.bank.repository;

import com.bank.model.*;
import com.bank.util.JsonUtil;
import com.bank.util.SecurityUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe In-Memory Repository with automatic JSON File Persistence.
 * Demonstrates Collections Framework (ConcurrentHashMap, CopyOnWriteArrayList, Streams),
 * Concurrency (ReentrantReadWriteLock), File I/O (NIO Paths/Files), and OOP Data Management.
 */
public class DataStore {

    private static DataStore instance;

    private final Map<String, User> usersById = new ConcurrentHashMap<>();
    private final Map<String, User> usersByUsername = new ConcurrentHashMap<>();
    private final Map<String, Account> accountsByNumber = new ConcurrentHashMap<>();
    private final List<Transaction> transactions = new CopyOnWriteArrayList<>();
    private final List<LoanApplication> loans = new CopyOnWriteArrayList<>();
    private final List<Investment> investments = new CopyOnWriteArrayList<>();
    private final List<AuditLog> auditLogs = new CopyOnWriteArrayList<>();
    private final Map<String, String> sessionTokenToUserId = new ConcurrentHashMap<>();
    private SystemSettings systemSettings = new SystemSettings();

    private final Path storagePath = Paths.get("data", "bank_data.json");
    private final ReentrantReadWriteLock fileLock = new ReentrantReadWriteLock();

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
            Files.createDirectories(storagePath.getParent());
            if (Files.exists(storagePath) && Files.size(storagePath) > 10) {
                loadFromFile();
            } else {
                seedInitialData();
                saveToFile();
            }
        } catch (Exception e) {
            System.err.println("[DataStore] Warning: Could not initialize persistence. Falling back to default seed. " + e.getMessage());
            seedInitialData();
        }
    }

    private void seedInitialData() {
        usersById.clear();
        usersByUsername.clear();
        accountsByNumber.clear();
        transactions.clear();
        loans.clear();
        investments.clear();
        auditLogs.clear();

        // 1. Admin User
        Admin admin = new Admin(
                "USR-ADMIN-01",
                "admin",
                SecurityUtil.hashPassword("admin123"),
                "Dr. Alexander Vance",
                "admin@novacorebank.com",
                "+1-800-555-0199",
                "Executive Operations",
                5
        );
        addUser(admin);

        // 2. Customer 1 - Alex Morgan
        Customer alex = new Customer(
                "USR-CUST-101",
                "alex.morgan",
                SecurityUtil.hashPassword("customer123"),
                "Alex Morgan",
                "alex.morgan@example.com",
                "+1-555-014-8891",
                "742 Evergreen Terrace, Springfield",
                "PAN-AM98741",
                "1234"
        );
        alex.setOccupation("Senior Software Architect");
        alex.setMonthlyIncome(12500.0);

        SavingsAccount alexSavings = new SavingsAccount("10010001", alex.getId(), 28540.50, "INR", 4.5, 500.0);
        alexSavings.setCardNumber("4532889912345678");
        alexSavings.setCardExpiry("10/28");
        alexSavings.setCardCvv("742");

        CheckingAccount alexChecking = new CheckingAccount("20010002", alex.getId(), 6420.00, "INR", 2500.0);
        alexChecking.setCardNumber("4532991187654321");
        alexChecking.setCardExpiry("05/29");
        alexChecking.setCardCvv("318");

        alex.addAccountNumber(alexSavings.getAccountNumber());
        alex.addAccountNumber(alexChecking.getAccountNumber());

        addUser(alex);
        addAccount(alexSavings);
        addAccount(alexChecking);

        // 3. Customer 2 - Priya Sharma
        Customer priya = new Customer(
                "USR-CUST-102",
                "priya.sharma",
                SecurityUtil.hashPassword("customer123"),
                "Priya Sharma",
                "priya.sharma@example.com",
                "+1-555-019-3321",
                "1204 Silicon Park Blvd, San Jose, CA",
                "PAN-PS55219",
                "4321"
        );
        priya.setOccupation("Financial Analyst");
        priya.setMonthlyIncome(9800.0);

        SavingsAccount priyaSavings = new SavingsAccount("10010003", priya.getId(), 14750.00, "INR", 4.5, 500.0);
        priyaSavings.setCardNumber("4532112233445566");
        priyaSavings.setCardExpiry("08/27");
        priyaSavings.setCardCvv("905");
        priya.addAccountNumber(priyaSavings.getAccountNumber());

        addUser(priya);
        addAccount(priyaSavings);

        // 4. Customer 3 - John Doe
        Customer john = new Customer(
                "USR-CUST-103",
                "john.doe",
                SecurityUtil.hashPassword("customer123"),
                "John Doe",
                "john.doe@example.com",
                "+1-555-017-6644",
                "450 Maple Avenue, Boston, MA",
                "PAN-JD10098",
                "1122"
        );
        john.setOccupation("Operations Consultant");
        john.setMonthlyIncome(7500.0);

        SavingsAccount johnSavings = new SavingsAccount("10010004", john.getId(), 8920.00, "INR", 4.5, 500.0);
        johnSavings.setCardNumber("4532667788990011");
        johnSavings.setCardExpiry("11/28");
        johnSavings.setCardCvv("442");
        john.addAccountNumber(johnSavings.getAccountNumber());

        addUser(john);
        addAccount(johnSavings);

        // Sample Transactions
        transactions.add(new Transaction("TXN-001", TransactionType.DEPOSIT, 5000.00, "EXTERNAL_GATEWAY", alexSavings.getAccountNumber(), "Salary Direct Deposit", 28540.50, alex.getId(), "REF-PAYROLL-01"));
        transactions.add(new Transaction("TXN-002", TransactionType.TRANSFER_OUT, 1200.00, alexSavings.getAccountNumber(), priyaSavings.getAccountNumber(), "Consulting Services Payment", 27340.50, alex.getId(), "REF-TXN-02"));
        transactions.add(new Transaction("TXN-003", TransactionType.TRANSFER_IN, 1200.00, alexSavings.getAccountNumber(), priyaSavings.getAccountNumber(), "Consulting Services Received", 14750.00, priya.getId(), "REF-TXN-02"));
        transactions.add(new Transaction("TXN-004", TransactionType.WITHDRAWAL, 350.00, alexChecking.getAccountNumber(), "ATM_BRANCH_07", "ATM Cash Withdrawal", 6420.00, alex.getId(), "REF-ATM-04"));
        transactions.add(new Transaction("TXN-005", TransactionType.DEPOSIT, 2500.00, "EXTERNAL_GATEWAY", johnSavings.getAccountNumber(), "Client Invoice Settlement", 8920.00, john.getId(), "REF-INV-05"));

        // Sample Loans
        LoanApplication loan1 = new LoanApplication("LOAN-701", alex.getId(), alex.getFullName(), "HOME", 150000.0, 120, 7.5, "New Family Residence Downpayment");
        loan1.setStatus("APPROVED");
        loan1.setDecidedAt("2026-09-15 10:30:00");
        loan1.setEmisPaid(4);
        loan1.setRemainingPrincipal(146200.0);
        loans.add(loan1);

        LoanApplication loan2 = new LoanApplication("LOAN-702", priya.getId(), priya.getFullName(), "EDUCATION", 25000.0, 36, 6.8, "Master Degree Executive Program");
        loan2.setStatus("PENDING");
        loans.add(loan2);

        // Sample Investments
        investments.add(new Investment("INV-901", alex.getId(), "FIXED_DEPOSIT", "Nova High-Yield 12M FD", 10000.0, 7.5, 12));
        investments.add(new Investment("INV-902", alex.getId(), "MUTUAL_FUND", "Global Tech Growth Index Fund", 5000.0, 11.2, 24));
        investments.add(new Investment("INV-903", priya.getId(), "GOLD_BOND", "Sovereign Gold Savings Series IV", 3500.0, 6.5, 36));

        // Audit Logs
        auditLogs.add(new AuditLog("LOG-001", "USR-ADMIN-01", "Dr. Alexander Vance", "ADMIN", "SYSTEM_INIT", "System initialized with production configuration and baseline accounts.", "127.0.0.1"));
        auditLogs.add(new AuditLog("LOG-002", "USR-ADMIN-01", "Dr. Alexander Vance", "ADMIN", "LOAN_APPROVAL", "Approved Home Loan LOAN-701 for customer Alex Morgan.", "127.0.0.1"));
    }

    public synchronized void saveToFile() {
        fileLock.writeLock().lock();
        try {
            Map<String, Object> root = new HashMap<>();

            List<Map<String, Object>> userList = new ArrayList<>();
            for (User u : usersById.values()) {
                Map<String, Object> umap = u.toMap();
                umap.put("passwordHash", u.getPasswordHash());
                userList.add(umap);
            }
            root.put("users", userList);

            List<Map<String, Object>> accList = new ArrayList<>();
            for (Account a : accountsByNumber.values()) {
                accList.add(a.toMap());
            }
            root.put("accounts", accList);

            List<Map<String, Object>> txList = new ArrayList<>();
            for (Transaction t : transactions) {
                txList.add(t.toMap());
            }
            root.put("transactions", txList);

            List<Map<String, Object>> loanList = new ArrayList<>();
            for (LoanApplication l : loans) {
                loanList.add(l.toMap());
            }
            root.put("loans", loanList);

            List<Map<String, Object>> invList = new ArrayList<>();
            for (Investment inv : investments) {
                invList.add(inv.toMap());
            }
            root.put("investments", invList);

            List<Map<String, Object>> logList = new ArrayList<>();
            for (AuditLog al : auditLogs) {
                logList.add(al.toMap());
            }
            root.put("auditLogs", logList);
            root.put("systemSettings", systemSettings.toMap());

            String json = JsonUtil.toJson(root);
            Files.writeString(storagePath, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            System.err.println("[DataStore] Failed to save database: " + e.getMessage());
        } finally {
            fileLock.writeLock().unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized void loadFromFile() {
        fileLock.readLock().lock();
        try {
            String json = Files.readString(storagePath, StandardCharsets.UTF_8);
            Map<String, Object> root = JsonUtil.parseObject(json);
            if (root == null || root.isEmpty()) {
                seedInitialData();
                return;
            }

            usersById.clear();
            usersByUsername.clear();
            accountsByNumber.clear();
            transactions.clear();
            loans.clear();
            investments.clear();
            auditLogs.clear();

            // Load Users
            List<Object> userList = (List<Object>) root.get("users");
            if (userList != null) {
                for (Object item : userList) {
                    Map<String, Object> m = (Map<String, Object>) item;
                    String role = (String) m.get("role");
                    String id = (String) m.get("id");
                    String username = (String) m.get("username");
                    String passwordHash = (String) m.get("passwordHash");
                    String fullName = (String) m.get("fullName");
                    String email = (String) m.get("email");
                    String phone = (String) m.get("phone");

                    if ("ADMIN".equalsIgnoreCase(role)) {
                        String dept = (String) m.getOrDefault("department", "Administration");
                        Number access = (Number) m.getOrDefault("accessLevel", 5);
                        Admin admin = new Admin(id, username, passwordHash, fullName, email, phone, dept, access.intValue());
                        if (m.containsKey("active")) admin.setActive(Boolean.TRUE.equals(m.get("active")));
                        addUser(admin);
                    } else {
                        String address = (String) m.getOrDefault("address", "");
                        String pan = (String) m.getOrDefault("panOrTaxId", "");
                        String pin = (String) m.getOrDefault("securityPin", "1234");
                        Customer customer = new Customer(id, username, passwordHash, fullName, email, phone, address, pan, pin);
                        if (m.containsKey("active")) customer.setActive(Boolean.TRUE.equals(m.get("active")));
                        if (m.containsKey("occupation")) customer.setOccupation((String) m.get("occupation"));
                        if (m.containsKey("monthlyIncome")) customer.setMonthlyIncome(((Number) m.get("monthlyIncome")).doubleValue());
                        List<Object> accs = (List<Object>) m.get("accountNumbers");
                        if (accs != null) {
                            for (Object o : accs) customer.addAccountNumber(o.toString());
                        }
                        addUser(customer);
                    }
                }
            }

            // Load Accounts
            List<Object> accList = (List<Object>) root.get("accounts");
            if (accList != null) {
                for (Object item : accList) {
                    Map<String, Object> m = (Map<String, Object>) item;
                    String type = (String) m.get("accountType");
                    String num = (String) m.get("accountNumber");
                    String custId = (String) m.get("customerId");
                    double bal = ((Number) m.getOrDefault("balance", 0.0)).doubleValue();
                    String curr = (String) m.getOrDefault("currency", "INR");

                    Account account;
                    if ("SAVINGS".equalsIgnoreCase(type)) {
                        double rate = ((Number) m.getOrDefault("interestRate", 4.5)).doubleValue();
                        double minBal = ((Number) m.getOrDefault("minimumBalance", 500.0)).doubleValue();
                        account = new SavingsAccount(num, custId, bal, curr, rate, minBal);
                    } else {
                        double od = ((Number) m.getOrDefault("overdraftLimit", 1000.0)).doubleValue();
                        account = new CheckingAccount(num, custId, bal, curr, od);
                    }
                    if (m.containsKey("status")) account.setStatus((String) m.get("status"));
                    if (m.containsKey("cardNumber")) account.setCardNumber((String) m.get("cardNumber"));
                    if (m.containsKey("cardExpiry")) account.setCardExpiry((String) m.get("cardExpiry"));
                    if (m.containsKey("cardCvv")) account.setCardCvv((String) m.get("cardCvv"));
                    if (m.containsKey("cardFrozen")) account.setCardFrozen(Boolean.TRUE.equals(m.get("cardFrozen")));
                    addAccount(account);
                }
            }

            // Load Transactions
            List<Object> txs = (List<Object>) root.get("transactions");
            if (txs != null) {
                for (Object item : txs) {
                    Map<String, Object> m = (Map<String, Object>) item;
                    Transaction t = new Transaction();
                    t.setId((String) m.get("id"));
                    t.setTimestamp((String) m.get("timestamp"));
                    String typeStr = (String) m.get("type");
                    if (typeStr != null) {
                        try { t.setType(TransactionType.valueOf(typeStr)); } catch (Exception ignored) {}
                    }
                    t.setAmount(((Number) m.getOrDefault("amount", 0.0)).doubleValue());
                    t.setFromAccount((String) m.get("fromAccount"));
                    t.setToAccount((String) m.get("toAccount"));
                    t.setDescription((String) m.get("description"));
                    t.setBalanceAfter(((Number) m.getOrDefault("balanceAfter", 0.0)).doubleValue());
                    t.setStatus((String) m.getOrDefault("status", "SUCCESS"));
                    t.setCustomerId((String) m.get("customerId"));
                    t.setReferenceNumber((String) m.get("referenceNumber"));
                    transactions.add(t);
                }
            }

            // Load Loans
            List<Object> lns = (List<Object>) root.get("loans");
            if (lns != null) {
                for (Object item : lns) {
                    Map<String, Object> m = (Map<String, Object>) item;
                    LoanApplication l = new LoanApplication();
                    l.setId((String) m.get("id"));
                    l.setCustomerId((String) m.get("customerId"));
                    l.setCustomerName((String) m.get("customerName"));
                    l.setLoanType((String) m.get("loanType"));
                    l.setAmount(((Number) m.getOrDefault("amount", 0.0)).doubleValue());
                    l.setTenureMonths(((Number) m.getOrDefault("tenureMonths", 12)).intValue());
                    l.setInterestRate(((Number) m.getOrDefault("interestRate", 8.0)).doubleValue());
                    l.setMonthlyEmi(((Number) m.getOrDefault("monthlyEmi", 0.0)).doubleValue());
                    l.setPurpose((String) m.get("purpose"));
                    l.setStatus((String) m.getOrDefault("status", "PENDING"));
                    l.setAppliedAt((String) m.get("appliedAt"));
                    l.setDecidedAt((String) m.get("decidedAt"));
                    l.setRemarks((String) m.get("remarks"));
                    l.setRemainingPrincipal(((Number) m.getOrDefault("remainingPrincipal", l.getAmount())).doubleValue());
                    l.setEmisPaid(((Number) m.getOrDefault("emisPaid", 0)).intValue());
                    loans.add(l);
                }
            }

            // Load Investments
            List<Object> invs = (List<Object>) root.get("investments");
            if (invs != null) {
                for (Object item : invs) {
                    Map<String, Object> m = (Map<String, Object>) item;
                    Investment inv = new Investment();
                    inv.setId((String) m.get("id"));
                    inv.setCustomerId((String) m.get("customerId"));
                    inv.setType((String) m.get("type"));
                    inv.setName((String) m.get("name"));
                    inv.setPrincipalAmount(((Number) m.getOrDefault("principalAmount", 0.0)).doubleValue());
                    inv.setInterestRate(((Number) m.getOrDefault("interestRate", 7.0)).doubleValue());
                    inv.setDurationMonths(((Number) m.getOrDefault("durationMonths", 12)).intValue());
                    inv.setExpectedReturn(((Number) m.getOrDefault("expectedReturn", 0.0)).doubleValue());
                    inv.setCurrentMaturityValue(((Number) m.getOrDefault("currentMaturityValue", 0.0)).doubleValue());
                    inv.setStartDate((String) m.get("startDate"));
                    inv.setMaturityDate((String) m.get("maturityDate"));
                    inv.setStatus((String) m.getOrDefault("status", "ACTIVE"));
                    investments.add(inv);
                }
            }

            // Load Audit Logs
            List<Object> logs = (List<Object>) root.get("auditLogs");
            if (logs != null) {
                for (Object item : logs) {
                    Map<String, Object> m = (Map<String, Object>) item;
                    AuditLog al = new AuditLog(
                            (String) m.get("id"),
                            (String) m.get("actorId"),
                            (String) m.get("actorName"),
                            (String) m.get("actorRole"),
                            (String) m.get("action"),
                            (String) m.get("details"),
                            (String) m.get("ipAddress")
                    );
                    al.setTimestamp((String) m.get("timestamp"));
                    auditLogs.add(al);
                }
            }

            // Load Settings
            Map<String, Object> set = (Map<String, Object>) root.get("systemSettings");
            if (set != null) {
                if (set.containsKey("bankName")) systemSettings.setBankName((String) set.get("bankName"));
                if (set.containsKey("defaultSavingsInterestRate")) systemSettings.setDefaultSavingsInterestRate(((Number) set.get("defaultSavingsInterestRate")).doubleValue());
                if (set.containsKey("defaultLoanInterestRate")) systemSettings.setDefaultLoanInterestRate(((Number) set.get("defaultLoanInterestRate")).doubleValue());
                if (set.containsKey("defaultFdInterestRate")) systemSettings.setDefaultFdInterestRate(((Number) set.get("defaultFdInterestRate")).doubleValue());
                if (set.containsKey("dailyTransferLimit")) systemSettings.setDailyTransferLimit(((Number) set.get("dailyTransferLimit")).doubleValue());
                if (set.containsKey("perTransactionLimit")) systemSettings.setPerTransactionLimit(((Number) set.get("perTransactionLimit")).doubleValue());
                if (set.containsKey("minSavingsBalance")) systemSettings.setMinSavingsBalance(((Number) set.get("minSavingsBalance")).doubleValue());
                if (set.containsKey("transactionFeePercent")) systemSettings.setTransactionFeePercent(((Number) set.get("transactionFeePercent")).doubleValue());
                if (set.containsKey("maintenanceMode")) systemSettings.setMaintenanceMode(Boolean.TRUE.equals(set.get("maintenanceMode")));
                if (set.containsKey("supportEmail")) systemSettings.setSupportEmail((String) set.get("supportEmail"));
                if (set.containsKey("supportPhone")) systemSettings.setSupportPhone((String) set.get("supportPhone"));
            }
        } catch (Exception e) {
            System.err.println("[DataStore] Failed to parse database file: " + e.getMessage());
            seedInitialData();
        } finally {
            fileLock.readLock().unlock();
        }
    }

    // CRUD Accessors
    public void addUser(User user) {
        usersById.put(user.getId(), user);
        usersByUsername.put(user.getUsername().toLowerCase(), user);
    }

    public void removeUser(String userId) {
        User user = usersById.remove(userId);
        if (user != null) {
            usersByUsername.remove(user.getUsername().toLowerCase());
        }
    }

    public User getUserById(String id) {
        return usersById.get(id);
    }

    public User getUserByUsername(String username) {
        if (username == null) return null;
        return usersByUsername.get(username.toLowerCase());
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(usersById.values());
    }

    public void addAccount(Account account) {
        accountsByNumber.put(account.getAccountNumber(), account);
    }

    public void removeAccount(String accountNumber) {
        accountsByNumber.remove(accountNumber);
    }

    public Account getAccountByNumber(String accountNumber) {
        return accountsByNumber.get(accountNumber);
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

    public void addTransaction(Transaction transaction) {
        transactions.add(0, transaction); // most recent first
    }

    public List<Transaction> getAllTransactions() {
        return new ArrayList<>(transactions);
    }

    public List<Transaction> getTransactionsByCustomerId(String customerId) {
        List<Transaction> res = new ArrayList<>();
        for (Transaction t : transactions) {
            if (customerId.equals(t.getCustomerId())) {
                res.add(t);
            }
        }
        return res;
    }

    public void addLoan(LoanApplication loan) {
        loans.add(0, loan);
    }

    public List<LoanApplication> getAllLoans() {
        return new ArrayList<>(loans);
    }

    public LoanApplication getLoanById(String id) {
        for (LoanApplication l : loans) {
            if (l.getId().equals(id)) return l;
        }
        return null;
    }

    public List<LoanApplication> getLoansByCustomerId(String customerId) {
        List<LoanApplication> res = new ArrayList<>();
        for (LoanApplication l : loans) {
            if (customerId.equals(l.getCustomerId())) {
                res.add(l);
            }
        }
        return res;
    }

    public void addInvestment(Investment investment) {
        investments.add(0, investment);
    }

    public List<Investment> getAllInvestments() {
        return new ArrayList<>(investments);
    }

    public Investment getInvestmentById(String id) {
        for (Investment inv : investments) {
            if (inv.getId().equals(id)) return inv;
        }
        return null;
    }

    public List<Investment> getInvestmentsByCustomerId(String customerId) {
        List<Investment> res = new ArrayList<>();
        for (Investment inv : investments) {
            if (customerId.equals(inv.getCustomerId())) {
                res.add(inv);
            }
        }
        return res;
    }

    public void addAuditLog(AuditLog log) {
        auditLogs.add(0, log);
    }

    public List<AuditLog> getAllAuditLogs() {
        return new ArrayList<>(auditLogs);
    }

    public SystemSettings getSystemSettings() {
        return systemSettings;
    }

    public void setSystemSettings(SystemSettings settings) {
        this.systemSettings = settings;
    }

    // Sessions
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
