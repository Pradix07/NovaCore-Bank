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
 * SERVICE: AccountService
 * ============================================================================
 * Demonstrates:
 * - Interface Implementation (implements IAccountService)
 * - Object-Oriented Polymorphism (SavingsAccount, CheckingAccount)
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
        User user = dataStore.getUserById(customerId);
        if (user == null || !(user instanceof Customer)) {
            throw new ValidationException("Invalid customer.");
        }
        Customer customer = (Customer) user;

        if (initialDeposit < 100.0) {
            throw new ValidationException("Minimum opening deposit is $100.00.");
        }

        String accType = "CHECKING".equalsIgnoreCase(accountType) ? "CHECKING" : "SAVINGS";
        String accNum = SecurityUtil.generateAccountNumber(accType);

        Account account;
        if ("SAVINGS".equals(accType)) {
            account = new SavingsAccount(accNum, customerId, initialDeposit, "INR", dataStore.getSystemSettings().getDefaultSavingsInterestRate(), dataStore.getSystemSettings().getMinSavingsBalance());
        } else {
            account = new CheckingAccount(accNum, customerId, initialDeposit, "INR", 1000.0);
        }

        account.setCardNumber(SecurityUtil.generateCardNumber());
        account.setCardExpiry(SecurityUtil.generateCardExpiry());
        account.setCardCvv(SecurityUtil.generateCvv());

        customer.addAccountNumber(accNum);
        dataStore.addAccount(account);

        // Transaction
        Transaction tx = new Transaction(
                SecurityUtil.generateId("TXN"),
                TransactionType.DEPOSIT,
                initialDeposit,
                "INITIAL_OPENING_DEPOSIT",
                accNum,
                "New " + accType + " Account Creation",
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
                "Opened " + accType + " account #" + accNum + " with deposit $" + initialDeposit,
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
                "Updated personal profile information and security settings.",
                "127.0.0.1"
        ));

        dataStore.saveToFile();
    }
}
