package com.bank.service;

import com.bank.exceptions.AccountNotFoundException;
import com.bank.exceptions.BankingException;
import com.bank.exceptions.ValidationException;
import com.bank.model.*;
import com.bank.repository.DataStore;
import com.bank.util.SecurityUtil;

import java.util.*;
import com.bank.service.interfaces.IAccountService;

/**
 * ============================================================================
 * SERVICE: AccountService (Indian Banking System)
 * ============================================================================
 * Demonstrates:
 * - Interface Implementation (implements IAccountService)
 * - Object-Oriented Polymorphism (SavingsAccount, CurrentAccount, StudentAccount, CheckingAccount)
 * - Indian Banking Rules (INR Currency, IFSC Code NOVA0001001, PAN/GSTIN/Student Verification)
 * - Security & Access Control Validation
 */
public class AccountService implements IAccountService {

    private final DataStore dataStore = DataStore.getInstance();

    public Map<String, Object> getCustomerDashboardOverview(String customerId) {
        User user = dataStore.getUserById(customerId);
        if (user == null) {
            throw new AccountNotFoundException("Customer profile not found.");
        }

        List<Account> accounts = dataStore.getAccountsByCustomerId(customerId);
        List<Transaction> transactions = dataStore.getTransactionsByCustomerId(customerId);
        List<LoanApplication> loans = dataStore.getLoansByCustomerId(customerId);
        List<Investment> investments = dataStore.getInvestmentsByCustomerId(customerId);

        double totalBalance = 0;
        double totalInflow = 0;
        double totalOutflow = 0;

        for (Account a : accounts) {
            totalBalance += a.getBalance();
        }

        for (Transaction t : transactions) {
            if ("SUCCESS".equalsIgnoreCase(t.getStatus())) {
                if (t.getType() == TransactionType.DEPOSIT || t.getType() == TransactionType.TRANSFER_IN || t.getType() == TransactionType.INVESTMENT_RETURN || t.getType() == TransactionType.LOAN_DISBURSEMENT) {
                    totalInflow += t.getAmount();
                } else {
                    totalOutflow += t.getAmount();
                }
            }
        }

        List<Map<String, Object>> accountMaps = new ArrayList<>();
        for (Account a : accounts) {
            accountMaps.add(a.toMap());
        }

        List<Map<String, Object>> recentTxMaps = new ArrayList<>();
        int count = 0;
        for (Transaction t : transactions) {
            recentTxMaps.add(t.toMap());
            if (++count >= 10) break;
        }

        List<Map<String, Object>> loanMaps = new ArrayList<>();
        for (LoanApplication l : loans) {
            loanMaps.add(l.toMap());
        }

        List<Map<String, Object>> invMaps = new ArrayList<>();
        for (Investment inv : investments) {
            invMaps.add(inv.toMap());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("profile", user.toMap());
        response.put("totalBalance", Math.round(totalBalance * 100.0) / 100.0);
        response.put("totalInflow", Math.round(totalInflow * 100.0) / 100.0);
        response.put("totalOutflow", Math.round(totalOutflow * 100.0) / 100.0);
        response.put("accounts", accountMaps);
        response.put("recentTransactions", recentTxMaps);
        response.put("loans", loanMaps);
        response.put("investments", invMaps);
        response.put("systemSettings", dataStore.getSystemSettings().toMap());
        return response;
    }

    @Override
    public List<Account> getCustomerAccounts(String customerId) {
        return dataStore.getAccountsByCustomerId(customerId);
    }

    @Override
    public Account getAccount(String accountNumber) throws AccountNotFoundException {
        Account acc = dataStore.getAccountByNumber(accountNumber);
        if (acc == null) {
            throw new AccountNotFoundException("Account #" + accountNumber + " not found.");
        }
        return acc;
    }

    @Override
    public Account openNewAccount(String customerId, String accountType, double initialDeposit) {
        return openNewAccountDetailed(customerId, accountType, initialDeposit, null, null);
    }

    /**
     * Enhanced detailed account opener supporting Indian Banking specific parameters.
     */
    public Account openNewAccountDetailed(String customerId, String accountType, double initialDeposit, String extra1, String extra2) {
        User user = dataStore.getUserById(customerId);
        if (user == null || !(user instanceof Customer)) {
            throw new ValidationException("Invalid customer profile.");
        }
        Customer customer = (Customer) user;

        String typeUpper = accountType != null ? accountType.toUpperCase().trim() : "SAVINGS";
        double minReq = "STUDENT".equals(typeUpper) ? 100.0 : ("CURRENT".equals(typeUpper) ? 5000.0 : 1000.0);

        if (initialDeposit < minReq) {
            throw new ValidationException("Minimum opening deposit for " + typeUpper + " account is ₹" + String.format("%,.2f", minReq));
        }

        String accNum = SecurityUtil.generateAccountNumber(typeUpper);
        Account account;

        if ("CURRENT".equals(typeUpper)) {
            String tradeLicenseOrGst = (extra1 != null && !extra1.trim().isEmpty()) ? extra1.trim() : "GSTIN-NOT-PROVIDED";
            String businessName = (extra2 != null && !extra2.trim().isEmpty()) ? extra2.trim() : customer.getFullName() + " Enterprises";
            account = new CurrentAccount(accNum, customerId, initialDeposit, "INR", tradeLicenseOrGst, businessName, 50000.0, 5000.0);
            account.setBranchName("Mumbai Fort Main Branch");
            account.setIfscCode("NOVA0001001");
        } else if ("STUDENT".equals(typeUpper)) {
            String institution = (extra1 != null && !extra1.trim().isEmpty()) ? extra1.trim() : "University / College";
            String studentId = (extra2 != null && !extra2.trim().isEmpty()) ? extra2.trim() : "STU-" + ((int)(Math.random() * 9000) + 1000);
            account = new StudentAccount(accNum, customerId, initialDeposit, "INR", institution, studentId, 0.035, 100.0, 100000.0, 20000.0);
            account.setBranchName("New Delhi Connaught Place Branch");
            account.setIfscCode("NOVA0003003");
        } else if ("CHECKING".equals(typeUpper)) {
            account = new CheckingAccount(accNum, customerId, initialDeposit, "INR", 25000.0);
            account.setBranchName("Mumbai Fort Main Branch");
            account.setIfscCode("NOVA0001001");
        } else {
            // Default: SAVINGS
            typeUpper = "SAVINGS";
            account = new SavingsAccount(accNum, customerId, initialDeposit, "INR", dataStore.getSystemSettings().getDefaultSavingsInterestRate(), dataStore.getSystemSettings().getMinSavingsBalance(), 50000.0);
            account.setBranchName("Mumbai Fort Main Branch");
            account.setIfscCode("NOVA0001001");
        }

        account.setCardNumber(SecurityUtil.generateCardNumber());
        account.setCardExpiry(SecurityUtil.generateCardExpiry());
        account.setCardCvv(SecurityUtil.generateCvv());
        account.setUpiId(customer.getUsername() + "@novabank");

        customer.addAccountNumber(accNum);
        dataStore.addAccount(account);

        // Record Opening Transaction
        Transaction tx = new Transaction(
                SecurityUtil.generateId("TXN"),
                TransactionType.DEPOSIT,
                initialDeposit,
                "INITIAL_OPENING_DEPOSIT",
                accNum,
                "New " + typeUpper + " Account Initial Funding",
                initialDeposit,
                customerId,
                SecurityUtil.generateReferenceNumber()
        );
        dataStore.addTransaction(tx);

        dataStore.addAuditLog(new AuditLog(
                SecurityUtil.generateId("LOG"),
                customerId,
                customer.getFullName(),
                "CUSTOMER",
                "NEW_ACCOUNT_CREATED",
                "Opened " + typeUpper + " account #" + accNum + " with deposit ₹" + String.format("%,.2f", initialDeposit) + " (IFSC: " + account.getIfscCode() + ")",
                "127.0.0.1"
        ));

        dataStore.saveToFile();
        return account;
    }

    public boolean toggleCardFreeze(String customerId, String accountNumber) {
        Account account = dataStore.getAccountByNumber(accountNumber);
        if (account == null || !account.getCustomerId().equals(customerId)) {
            throw new AccountNotFoundException("Account not found or access denied.");
        }

        boolean newStatus = !account.isCardFrozen();
        account.setCardFrozen(newStatus);
        dataStore.saveToFile();
        return newStatus;
    }

    public void updateCustomerProfile(String customerId, String fullName, String email, String phone, String address, String currentPassword, String newPassword, String securityPin) {
        User user = dataStore.getUserById(customerId);
        if (user == null || !(user instanceof Customer)) {
            throw new ValidationException("User not found.");
        }
        Customer customer = (Customer) user;

        if (fullName != null && !fullName.trim().isEmpty()) customer.setFullName(fullName.trim());
        if (email != null && email.contains("@")) customer.setEmail(email.trim());
        if (phone != null) customer.setPhone(phone.trim());
        if (address != null) customer.setAddress(address.trim());
        if (securityPin != null && securityPin.trim().length() == 4) customer.setSecurityPin(securityPin.trim());

        if (newPassword != null && !newPassword.trim().isEmpty()) {
            if (currentPassword == null || !SecurityUtil.verifyPassword(currentPassword, customer.getPasswordHash())) {
                throw new ValidationException("Current password verification failed.");
            }
            if (newPassword.trim().length() < 6) {
                throw new ValidationException("New password must be at least 6 characters.");
            }
            customer.setPasswordHash(SecurityUtil.hashPassword(newPassword.trim()));
        }

        dataStore.addAuditLog(new AuditLog(
                SecurityUtil.generateId("LOG"),
                customerId,
                customer.getFullName(),
                "CUSTOMER",
                "PROFILE_UPDATE",
                "Updated personal profile information and KYC security settings.",
                "127.0.0.1"
        ));

        dataStore.saveToFile();
    }
}
