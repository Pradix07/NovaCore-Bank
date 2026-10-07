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
 * ============================================================================
 * DATA STORE (Thread-Safe Persistence & In-Memory Repository)
 * ============================================================================
 * Indian Banking System Data Store:
 * - Persists to data/bank_data.json
 * - Manages Savings, Current, Student, and Checking Accounts
 * - Supports Indian KYC (PAN, Aadhaar, UPI ID, IFSC)
 * - Thread-safe operations using ConcurrentHashMap, CopyOnWriteArrayList, ReentrantReadWriteLock
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
        transactions.add(new Transaction("TXN-IN-001", TransactionType.DEPOSIT, 50000.00, "SALARY_CREDIT_IMPS", aaravSavings.getAccountNumber(), "Monthly Salary Credit via IMPS", 74500.00, aarav.getId(), "IMPS92810394812"));
        transactions.add(new Transaction("TXN-IN-002", TransactionType.TRANSFER_OUT, 15000.00, aaravSavings.getAccountNumber(), priyaSavings.getAccountNumber(), "UPI Transfer to Priya Sharma (NOVA0002002)", 59500.00, aarav.getId(), "UPI202610078891"));
        transactions.add(new Transaction("TXN-IN-003", TransactionType.TRANSFER_IN, 15000.00, aaravSavings.getAccountNumber(), priyaSavings.getAccountNumber(), "UPI Payment Received from Aarav Patel", 58250.00, priya.getId(), "UPI202610078891"));
        transactions.add(new Transaction("TXN-IN-004", TransactionType.WITHDRAWAL, 5000.00, aaravCurrent.getAccountNumber(), "ATM_MUMBAI_FORT", "RuPay ATM Cash Withdrawal", 150000.00, aarav.getId(), "ATM20269988112"));
        transactions.add(new Transaction("TXN-IN-005", TransactionType.DEPOSIT, 10000.00, "SCHOLARSHIP_NEFT", rohanStudent.getAccountNumber(), "National Merit Scholarship NEFT Disbursal", 18450.00, rohan.getId(), "NEFT9081237461"));

        // Sample Loans (INR)
        LoanApplication loan1 = new LoanApplication("LOAN-IN-701", aarav.getId(), aarav.getFullName(), "HOME", 4500000.0, 180, 8.4, "Apartment Purchase in Mumbai Suburban");
        loan1.setStatus("APPROVED");
        loan1.setDecidedAt("2026-09-15 10:30:00");
        loan1.setEmisPaid(6);
        loan1.setRemainingPrincipal(4420000.0);
        loans.add(loan1);

        LoanApplication loan2 = new LoanApplication("LOAN-IN-702", priya.getId(), priya.getFullName(), "EDUCATION", 800000.0, 48, 7.2, "Post Graduate Financial Analytics Degree");
        loan2.setStatus("PENDING");
        loans.add(loan2);

        // Sample Indian Investments
        investments.add(new Investment("INV-IN-901", aarav.getId(), "FIXED_DEPOSIT", "Nova Shrestha 1-Year FD (7.5% p.a.)", 100000.0, 7.5, 12));
        investments.add(new Investment("INV-IN-902", aarav.getId(), "MUTUAL_FUND", "Nifty 50 Bluechip Index Growth Fund", 50000.0, 12.5, 24));
        investments.add(new Investment("INV-IN-903", priya.getId(), "GOLD_BOND", "RBI Sovereign Gold Bond (SGB) Series 2026", 75000.0, 6.5, 36));

        // Audit Logs
        auditLogs.add(new AuditLog("LOG-IN-001", "USR-ADMIN-01", "Rajesh Kumar Sharma", "ADMIN", "SYSTEM_INIT", "NovaCore Bank of India core platform initialized with IFSC NOVA0001001.", "127.0.0.1"));
        auditLogs.add(new AuditLog("LOG-IN-002", "USR-ADMIN-01", "Rajesh Kumar Sharma", "ADMIN", "LOAN_APPROVAL", "Approved Home Loan LOAN-IN-701 for customer Aarav Patel (₹45,00,000.00).", "127.0.0.1"));
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
                        String dept = (String) m.getOrDefault("department", "Banking Operations");
                        Number access = (Number) m.getOrDefault("accessLevel", 5);
                        Admin admin = new Admin(id, username, passwordHash, fullName, email, phone, dept, access.intValue());
                        if (m.containsKey("active")) admin.setActive(Boolean.TRUE.equals(m.get("active")));
                        addUser(admin);
                    } else {
                        String address = (String) m.getOrDefault("address", "");
                        String pan = (String) m.getOrDefault("panOrTaxId", "");
                        String aadhaar = (String) m.getOrDefault("aadhaarNumber", "XXXX-XXXX-1234");
                        String upi = (String) m.getOrDefault("upiId", username + "@novabank");
                        String pin = (String) m.getOrDefault("securityPin", "1234");
                        Customer customer = new Customer(id, username, passwordHash, fullName, email, phone, address, pan, aadhaar, upi, pin);
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
                        double rate = ((Number) m.getOrDefault("interestRate", 0.040)).doubleValue();
                        double minBal = ((Number) m.getOrDefault("minimumBalance", 1000.0)).doubleValue();
                        double maxWith = ((Number) m.getOrDefault("maxWithdrawLimit", 50000.0)).doubleValue();
                        account = new SavingsAccount(num, custId, bal, curr, rate, minBal, maxWith);
                    } else if ("CURRENT".equalsIgnoreCase(type)) {
                        String trade = (String) m.getOrDefault("tradeLicenseOrGst", "GSTIN-PENDING");
                        String bName = (String) m.getOrDefault("businessName", "Commercial Enterprise");
                        double od = ((Number) m.getOrDefault("overdraftLimit", 50000.0)).doubleValue();
                        double minBal = ((Number) m.getOrDefault("minimumBalance", 5000.0)).doubleValue();
                        account = new CurrentAccount(num, custId, bal, curr, trade, bName, od, minBal);
                    } else if ("STUDENT".equalsIgnoreCase(type)) {
                        String inst = (String) m.getOrDefault("institutionName", "Educational Institution");
                        String stuId = (String) m.getOrDefault("studentId", "STU-0000");
                        double rate = ((Number) m.getOrDefault("interestRate", 0.035)).doubleValue();
                        double minBal = ((Number) m.getOrDefault("minimumBalance", 100.0)).doubleValue();
                        double maxBal = ((Number) m.getOrDefault("maxBalanceLimit", 100000.0)).doubleValue();
                        double maxWith = ((Number) m.getOrDefault("maxWithdrawLimit", 20000.0)).doubleValue();
                        account = new StudentAccount(num, custId, bal, curr, inst, stuId, rate, minBal, maxBal, maxWith);
                    } else {
                        double od = ((Number) m.getOrDefault("overdraftLimit", 25000.0)).doubleValue();
                        account = new CheckingAccount(num, custId, bal, curr, od);
                    }

                    if (m.containsKey("ifscCode")) account.setIfscCode((String) m.get("ifscCode"));
                    if (m.containsKey("branchName")) account.setBranchName((String) m.get("branchName"));
                    if (m.containsKey("upiId")) account.setUpiId((String) m.get("upiId"));
                    if (m.containsKey("nomineeName")) account.setNomineeName((String) m.get("nomineeName"));
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
                    l.setInterestRate(((Number) m.getOrDefault("interestRate", 8.4)).doubleValue());
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
                    inv.setInterestRate(((Number) m.getOrDefault("interestRate", 7.5)).doubleValue());
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
