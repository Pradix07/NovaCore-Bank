package com.bank.model;

import com.bank.exceptions.AccountNotFoundException;
import com.bank.exceptions.InsufficientFundsException;
import com.bank.exceptions.InvalidAmountException;
import com.bank.exceptions.MaxBalanceLimitException;
import com.bank.exceptions.MaxWithdrawLimitException;
import com.bank.repository.DataStore;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.DefaultListModel;

/**
 * ============================================================================
 * BANK CORE MANAGER (Matching Reference Architecture)
 * ============================================================================
 * Provides centralized collection management and transactional dispatch for:
 * - Savings Account creation
 * - Current Account creation (with Trade License / GSTIN)
 * - Student Account creation (with Institution Name & Student ID)
 * - Thread-safe Deposit, Withdrawal, Transfer, and Find operations
 * - DefaultListModel and Collection models for GUI passbook rendering
 */
public class Bank implements Serializable {
    private static final long serialVersionUID = 1L;

    private String bankName = "NovaCore Bank of India";
    private String ifscPrefix = "NOVA0001001";
    private final List<Account> accounts = new CopyOnWriteArrayList<>();

    public Bank() {
        this.bankName = "NovaCore Bank of India";
        syncWithDataStore();
    }

    public Bank(String bankName, String ifscPrefix) {
        this.bankName = bankName;
        this.ifscPrefix = ifscPrefix;
        syncWithDataStore();
    }

    /**
     * Synchronizes internal memory bank cache with DataStore.
     */
    public void syncWithDataStore() {
        accounts.clear();
        accounts.addAll(DataStore.getInstance().getAllAccounts());
    }

    /**
     * Adds an account to the bank.
     */
    public int addAccount(Account acc) {
        if (acc == null) return -1;
        accounts.add(acc);
        DataStore.getInstance().addAccount(acc);
        DataStore.getInstance().saveToFile();
        return accounts.size() - 1;
    }

    /**
     * Creates and adds a Savings Account (Indian Banking Standard).
     */
    public SavingsAccount addSavingsAccount(String customerId, String name, double balance, double interestRate, double minBalance) {
        String accNum = generateUniqueAccountNumber("1001");
        SavingsAccount acc = new SavingsAccount(accNum, customerId, balance, "INR", interestRate > 0 ? interestRate : 0.040, minBalance > 0 ? minBalance : 1000.0);
        acc.setIfscCode(ifscPrefix);
        addAccount(acc);
        return acc;
    }

    /**
     * Creates and adds a Current Account (Business / Enterprise with GSTIN or Trade License).
     */
    public CurrentAccount addCurrentAccount(String customerId, String name, double balance, String tradeLicenseOrGst, String businessName, double overdraftLimit) {
        String accNum = generateUniqueAccountNumber("2001");
        CurrentAccount acc = new CurrentAccount(accNum, customerId, balance, "INR", tradeLicenseOrGst, businessName != null ? businessName : name, overdraftLimit > 0 ? overdraftLimit : 50000.0, 5000.0);
        acc.setIfscCode(ifscPrefix);
        addAccount(acc);
        return acc;
    }

    /**
     * Creates and adds a Student Account (Zero-Balance / Concession Account).
     */
    public StudentAccount addStudentAccount(String customerId, String name, double balance, String institutionName, String studentId) {
        String accNum = generateUniqueAccountNumber("3001");
        StudentAccount acc = new StudentAccount(accNum, customerId, balance, "INR", institutionName, studentId);
        acc.setIfscCode(ifscPrefix);
        addAccount(acc);
        return acc;
    }

    /**
     * Finds an account by account number.
     */
    public Account findAccount(String accountNumber) {
        if (accountNumber == null) return null;
        Account acc = DataStore.getInstance().getAccountByNumber(accountNumber.trim());
        if (acc != null) return acc;

        for (Account a : accounts) {
            if (a.getAccountNumber().equalsIgnoreCase(accountNumber.trim())) {
                return a;
            }
        }
        return null;
    }

    /**
     * Deposits funds into an account.
     */
    public void deposit(String accountNumber, double amount) throws InvalidAmountException, AccountNotFoundException {
        if (amount <= 0) {
            throw new InvalidAmountException("Deposit amount must be greater than zero. Received: ₹" + amount);
        }
        Account acc = findAccount(accountNumber);
        if (acc == null) {
            throw new AccountNotFoundException("Account number '" + accountNumber + "' does not exist in NovaCore Bank of India.");
        }
        acc.deposit(amount);
        DataStore.getInstance().saveToFile();
    }

    /**
     * Withdraws funds from an account.
     */
    public void withdraw(String accountNumber, double amount)
            throws AccountNotFoundException, InvalidAmountException, InsufficientFundsException, MaxWithdrawLimitException, MaxBalanceLimitException {
        if (amount <= 0) {
            throw new InvalidAmountException("Withdrawal amount must be greater than zero. Received: ₹" + amount);
        }
        Account acc = findAccount(accountNumber);
        if (acc == null) {
            throw new AccountNotFoundException("Account number '" + accountNumber + "' does not exist in NovaCore Bank of India.");
        }
        acc.withdraw(amount);
        DataStore.getInstance().saveToFile();
    }

    /**
     * Displays all accounts formatted for Swing GUI components (DefaultListModel).
     */
    public DefaultListModel<String> display() {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        List<Account> all = DataStore.getInstance().getAllAccounts();
        for (Account a : all) {
            listModel.addElement(String.format("[%s] Acc: %s | IFSC: %s | Bal: ₹%,.2f | Status: %s",
                    a.getAccountType(), a.getAccountNumber(), a.getIfscCode(), a.getBalance(), a.getStatus()));
        }
        return listModel;
    }

    /**
     * Helper to generate unique Indian standard account number.
     */
    private String generateUniqueAccountNumber(String prefix) {
        String num;
        do {
            num = prefix + String.format("%08d", (int) (Math.random() * 90000000) + 10000000);
        } while (findAccount(num) != null);
        return num;
    }

    // Getters and Setters
    public List<Account> getAccounts() {
        return new ArrayList<>(DataStore.getInstance().getAllAccounts());
    }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getIfscPrefix() { return ifscPrefix; }
    public void setIfscPrefix(String ifscPrefix) { this.ifscPrefix = ifscPrefix; }
}
