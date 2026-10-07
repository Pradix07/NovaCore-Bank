package com.bank.model;

import com.bank.exceptions.InsufficientFundsException;
import com.bank.exceptions.InvalidAmountException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ============================================================================
 * ABSTRACT BASE CLASS: Account
 * ============================================================================
 * Demonstrates:
 * - OOP Principle: Abstraction & Inheritance
 * - OOP Principle: Interface Implementation (AccountOperations)
 * - OOP Principle: Encapsulation (protected fields with public getters/setters)
 * - Concurrency: Thread-Safe Concurrency control with ReentrantLock
 * - Indian Banking System: INR Currency, IFSC Code, Branch, UPI ID, RuPay/Debit Card
 */
public abstract class Account implements AccountOperations, Serializable {
    private static final long serialVersionUID = 1L;

    protected String accountNumber;
    protected String customerId;
    protected double balance;
    protected String currency; // "INR"
    protected String status;   // "ACTIVE", "FROZEN", "CLOSED"
    protected String ifscCode; // e.g. "NOVA0001001"
    protected String branchName; // e.g. "Mumbai Fort Main Branch"
    protected String upiId;      // e.g. "customer@novabank"
    protected String nomineeName;
    protected String createdAt;
    protected String cardNumber;
    protected String cardExpiry;
    protected String cardCvv;
    protected boolean cardFrozen;
    
    // Concurrency lock to guarantee thread-safe balance transactions (Multithreading)
    protected transient ReentrantLock lock = new ReentrantLock();

    public Account() {
        this.currency = "INR";
        this.status = "ACTIVE";
        this.ifscCode = "NOVA0001001";
        this.branchName = "Mumbai Central Main Branch";
        this.createdAt = LocalDateTime.now().toString();
        this.cardFrozen = false;
    }

    public Account(String accountNumber, String customerId, double initialBalance, String currency) {
        this();
        this.accountNumber = accountNumber;
        this.customerId = customerId;
        this.balance = initialBalance;
        if (currency != null && !currency.trim().isEmpty()) {
            this.currency = currency;
        }
    }

    public ReentrantLock getLock() {
        if (lock == null) {
            lock = new ReentrantLock();
        }
        return lock;
    }

    // Abstract methods demonstrating Polymorphism
    @Override
    public abstract String getAccountType();

    @Override
    public abstract double calculateAnnualInterest();

    @Override
    public abstract void withdraw(double amount) throws InsufficientFundsException;

    @Override
    public void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero. Received: ₹" + amount);
        }
        getLock().lock();
        try {
            this.balance += amount;
        } finally {
            getLock().unlock();
        }
    }

    public abstract Map<String, Object> toMap();

    protected Map<String, Object> getBaseMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("accountNumber", accountNumber);
        map.put("customerId", customerId);
        map.put("balance", balance);
        map.put("currency", currency);
        map.put("status", status);
        map.put("accountType", getAccountType());
        map.put("ifscCode", ifscCode);
        map.put("branchName", branchName);
        map.put("upiId", upiId != null ? upiId : (accountNumber + "@novabank"));
        map.put("nomineeName", nomineeName != null ? nomineeName : "Not Nominated");
        map.put("createdAt", createdAt);
        map.put("cardNumber", cardNumber);
        map.put("cardExpiry", cardExpiry);
        map.put("cardCvv", cardCvv);
        map.put("cardFrozen", cardFrozen);
        return map;
    }

    // Getters and Setters
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }

    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }

    public String getNomineeName() { return nomineeName; }
    public void setNomineeName(String nomineeName) { this.nomineeName = nomineeName; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public String getCardExpiry() { return cardExpiry; }
    public void setCardExpiry(String cardExpiry) { this.cardExpiry = cardExpiry; }

    public String getCardCvv() { return cardCvv; }
    public void setCardCvv(String cardCvv) { this.cardCvv = cardCvv; }

    public boolean isCardFrozen() { return cardFrozen; }
    public void setCardFrozen(boolean cardFrozen) { this.cardFrozen = cardFrozen; }

    @Override
    public String toString() {
        return String.format("[%s] Acc: %s | IFSC: %s | Bal: ₹%,.2f | Status: %s",
                getAccountType(), accountNumber, ifscCode, balance, status);
    }
}
