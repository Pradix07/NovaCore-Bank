package com.bank.model;

import com.bank.exceptions.InsufficientFundsException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Abstract base class for Bank Accounts.
 * Demonstrates:
 * - OOP Principle: Abstraction & Inheritance
 * - OOP Principle: Interface Implementation (AccountOperations)
 * - OOP Principle: Encapsulation (protected fields with public getters/setters)
 * - Concurrency: Thread-Safe Concurrency control with ReentrantLock
 */
public abstract class Account implements AccountOperations, Serializable {
    private static final long serialVersionUID = 1L;

    protected String accountNumber;
    protected String customerId;
    protected double balance;
    protected String currency;
    protected String status; // ACTIVE, FROZEN, CLOSED
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
        this.createdAt = LocalDateTime.now().toString();
        this.cardFrozen = false;
    }

    public Account(String accountNumber, String customerId, double initialBalance, String currency) {
        this();
        this.accountNumber = accountNumber;
        this.customerId = customerId;
        this.balance = initialBalance;
        this.currency = currency;
    }

    public ReentrantLock getLock() {
        if (lock == null) {
            lock = new ReentrantLock();
        }
        return lock;
    }

    // Abstract methods demonstrating Polymorphism
    public abstract String getAccountType();
    public abstract double calculateAnnualInterest();
    public abstract void withdraw(double amount) throws InsufficientFundsException;

    public void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero.");
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
}
