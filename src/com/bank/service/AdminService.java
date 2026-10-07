package com.bank.service;

import com.bank.exceptions.BankingException;
import com.bank.exceptions.ValidationException;
import com.bank.model.*;
import com.bank.repository.DataStore;
import com.bank.util.SecurityUtil;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import com.bank.service.interfaces.IAdminService;

/**
 * ============================================================================
 * SERVICE: AdminService
 * ============================================================================
 * Demonstrates:
 * - Interface Implementation (implements IAdminService)
 * - Java 8 Streams, mapToDouble, filtering, and reduction
 * - Role-based authorization & security audit logging
 */
public class AdminService implements IAdminService {

    private final DataStore dataStore = DataStore.getInstance();

    public Map<String, Object> getAdminDashboardMetrics() {
        List<User> users = dataStore.getAllUsers();
        List<Account> accounts = dataStore.getAllAccounts();
        List<Transaction> transactions = dataStore.getAllTransactions();
        List<LoanApplication> loans = dataStore.getAllLoans();
        List<Investment> investments = dataStore.getAllInvestments();

        double totalDeposits = accounts.stream().mapToDouble(Account::getBalance).sum();
        long totalCustomers = users.stream().filter(u -> "CUSTOMER".equalsIgnoreCase(u.getRole())).count();
        long activeAccounts = accounts.stream().filter(a -> "ACTIVE".equalsIgnoreCase(a.getStatus())).count();
        double totalLoanDisbursed = loans.stream().filter(l -> "APPROVED".equalsIgnoreCase(l.getStatus())).mapToDouble(LoanApplication::getAmount).sum();
        double totalInvestments = investments.stream().filter(i -> "ACTIVE".equalsIgnoreCase(i.getStatus())).mapToDouble(Investment::getPrincipalAmount).sum();
        long pendingLoansCount = loans.stream().filter(l -> "PENDING".equalsIgnoreCase(l.getStatus())).count();

        // Calculate transaction volume breakdown
        double totalVolume = transactions.stream().mapToDouble(Transaction::getAmount).sum();

        // Daily Transaction stats for chart
        Map<String, Double> volumeByDate = new LinkedHashMap<>();
        Map<String, Integer> countByType = new HashMap<>();

        for (Transaction t : transactions) {
            String date = t.getTimestamp() != null && t.getTimestamp().length() >= 10 ? t.getTimestamp().substring(0, 10) : "Today";
            volumeByDate.put(date, volumeByDate.getOrDefault(date, 0.0) + t.getAmount());

            String type = t.getType() != null ? t.getType().name() : "OTHER";
            countByType.put(type, countByType.getOrDefault(type, 0) + 1);
        }

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalDeposits", Math.round(totalDeposits * 100.0) / 100.0);
        metrics.put("totalCustomers", totalCustomers);
        metrics.put("activeAccounts", activeAccounts);
        metrics.put("totalLoanDisbursed", Math.round(totalLoanDisbursed * 100.0) / 100.0);
        metrics.put("totalInvestments", Math.round(totalInvestments * 100.0) / 100.0);
        metrics.put("pendingLoansCount", pendingLoansCount);
        metrics.put("totalTransactionsCount", transactions.size());
        metrics.put("totalTransactionVolume", Math.round(totalVolume * 100.0) / 100.0);
        metrics.put("volumeByDate", volumeByDate);
        metrics.put("countByType", countByType);
        metrics.put("systemSettings", dataStore.getSystemSettings().toMap());

        return metrics;
    }

    public List<Map<String, Object>> getAllUsersWithDetails() {
        List<User> users = dataStore.getAllUsers();
        List<Map<String, Object>> result = new ArrayList<>();

        for (User u : users) {
            Map<String, Object> map = u.toMap();
            if (u instanceof Customer) {
                List<Account> accs = dataStore.getAccountsByCustomerId(u.getId());
                List<Map<String, Object>> accList = new ArrayList<>();
                double custBalance = 0;
                for (Account a : accs) {
                    accList.add(a.toMap());
                    custBalance += a.getBalance();
                }
                map.put("accounts", accList);
                map.put("totalCustomerBalance", custBalance);
            }
            result.add(map);
        }
        return result;
    }

    public User createUser(String adminId, String username, String password, String fullName, String email, String phone, String role, String initialAccountType, double initialBalance, String address, String panOrTaxId) {
        if (username == null || username.trim().length() < 3) {
            throw new ValidationException("Username must be at least 3 characters.");
        }
        if (password == null || password.trim().length() < 6) {
            throw new ValidationException("Password must be at least 6 characters.");
        }
        if (dataStore.getUserByUsername(username.trim()) != null) {
            throw new ValidationException("Username already exists.");
        }

        User user;
        String newId = "ADMIN".equalsIgnoreCase(role) ? SecurityUtil.generateId("USR-ADMIN") : SecurityUtil.generateId("USR-CUST");
        String passHash = SecurityUtil.hashPassword(password.trim());

        if ("ADMIN".equalsIgnoreCase(role)) {
            user = new Admin(newId, username.trim(), passHash, fullName.trim(), email.trim(), phone != null ? phone.trim() : "", "Administration", 4);
        } else {
            Customer cust = new Customer(newId, username.trim(), passHash, fullName.trim(), email.trim(), phone != null ? phone.trim() : "", address != null ? address.trim() : "", panOrTaxId != null ? panOrTaxId.trim() : "", "1234");
            
            // Create initial account for customer
            String accType = "CHECKING".equalsIgnoreCase(initialAccountType) ? "CHECKING" : "SAVINGS";
            String accNum = SecurityUtil.generateAccountNumber(accType);
            double bal = Math.max(initialBalance, 500.0);

            Account account;
            if ("SAVINGS".equals(accType)) {
                account = new SavingsAccount(accNum, newId, bal, "INR", dataStore.getSystemSettings().getDefaultSavingsInterestRate(), dataStore.getSystemSettings().getMinSavingsBalance());
            } else {
                account = new CheckingAccount(accNum, newId, bal, "INR", 1000.0);
            }
            account.setCardNumber(SecurityUtil.generateCardNumber());
            account.setCardExpiry(SecurityUtil.generateCardExpiry());
            account.setCardCvv(SecurityUtil.generateCvv());

            cust.addAccountNumber(accNum);
            dataStore.addAccount(account);

            Transaction initTx = new Transaction(
                    SecurityUtil.generateId("TXN"),
                    TransactionType.DEPOSIT,
                    bal,
                    "ADMIN_SYSTEM_CREATION",
                    accNum,
                    "Admin Created Account Deposit",
                    bal,
                    newId,
                    SecurityUtil.generateReferenceNumber()
            );
            dataStore.addTransaction(initTx);
            user = cust;
        }

        dataStore.addUser(user);
        dataStore.addAuditLog(new AuditLog(
                SecurityUtil.generateId("LOG"),
                adminId,
                "Admin Operations",
                "ADMIN",
                "USER_CREATED",
                "Created new user " + user.getUsername() + " (" + user.getRole() + ")",
                "127.0.0.1"
        ));

        dataStore.saveToFile();
        return user;
    }

    public void updateUserStatus(String adminId, String targetUserId, boolean active) {
        User user = dataStore.getUserById(targetUserId);
        if (user == null) {
            throw new ValidationException("User not found.");
        }
        user.setActive(active);

        // Also freeze/unfreeze customer accounts
        if (user instanceof Customer) {
            List<Account> accounts = dataStore.getAccountsByCustomerId(targetUserId);
            for (Account a : accounts) {
                a.setStatus(active ? "ACTIVE" : "FROZEN");
            }
        }

        dataStore.addAuditLog(new AuditLog(
                SecurityUtil.generateId("LOG"),
                adminId,
                "Admin Operations",
                "ADMIN",
                "USER_STATUS_CHANGE",
                "Updated status for user " + user.getUsername() + " to " + (active ? "ACTIVE" : "SUSPENDED"),
                "127.0.0.1"
        ));

        dataStore.saveToFile();
    }

    public void deleteUser(String adminId, String targetUserId) {
        User user = dataStore.getUserById(targetUserId);
        if (user == null) {
            throw new ValidationException("User not found.");
        }
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            throw new ValidationException("Default super administrator account cannot be deleted.");
        }

        dataStore.removeUser(targetUserId);

        // Remove associated accounts
        if (user instanceof Customer) {
            List<Account> accounts = dataStore.getAccountsByCustomerId(targetUserId);
            for (Account a : accounts) {
                dataStore.removeAccount(a.getAccountNumber());
            }
        }

        dataStore.addAuditLog(new AuditLog(
                SecurityUtil.generateId("LOG"),
                adminId,
                "Admin Operations",
                "ADMIN",
                "USER_DELETED",
                "Deleted user account " + user.getUsername() + " (" + targetUserId + ")",
                "127.0.0.1"
        ));

        dataStore.saveToFile();
    }

    public void updateSystemSettings(String adminId, Map<String, Object> newSettings) {
        SystemSettings settings = dataStore.getSystemSettings();

        if (newSettings.containsKey("bankName")) settings.setBankName((String) newSettings.get("bankName"));
        if (newSettings.containsKey("defaultSavingsInterestRate")) settings.setDefaultSavingsInterestRate(((Number) newSettings.get("defaultSavingsInterestRate")).doubleValue());
        if (newSettings.containsKey("defaultLoanInterestRate")) settings.setDefaultLoanInterestRate(((Number) newSettings.get("defaultLoanInterestRate")).doubleValue());
        if (newSettings.containsKey("defaultFdInterestRate")) settings.setDefaultFdInterestRate(((Number) newSettings.get("defaultFdInterestRate")).doubleValue());
        if (newSettings.containsKey("dailyTransferLimit")) settings.setDailyTransferLimit(((Number) newSettings.get("dailyTransferLimit")).doubleValue());
        if (newSettings.containsKey("perTransactionLimit")) settings.setPerTransactionLimit(((Number) newSettings.get("perTransactionLimit")).doubleValue());
        if (newSettings.containsKey("minSavingsBalance")) settings.setMinSavingsBalance(((Number) newSettings.get("minSavingsBalance")).doubleValue());
        if (newSettings.containsKey("transactionFeePercent")) settings.setTransactionFeePercent(((Number) newSettings.get("transactionFeePercent")).doubleValue());
        if (newSettings.containsKey("maintenanceMode")) settings.setMaintenanceMode(Boolean.TRUE.equals(newSettings.get("maintenanceMode")));
        if (newSettings.containsKey("supportEmail")) settings.setSupportEmail((String) newSettings.get("supportEmail"));
        if (newSettings.containsKey("supportPhone")) settings.setSupportPhone((String) newSettings.get("supportPhone"));

        dataStore.addAuditLog(new AuditLog(
                SecurityUtil.generateId("LOG"),
                adminId,
                "Admin Operations",
                "ADMIN",
                "SETTINGS_UPDATED",
                "System configuration and financial rates updated.",
                "127.0.0.1"
        ));

        dataStore.saveToFile();
    }

    public List<Map<String, Object>> getAllAuditLogs() {
        return dataStore.getAllAuditLogs().stream().map(AuditLog::toMap).collect(Collectors.toList());
    }
}
