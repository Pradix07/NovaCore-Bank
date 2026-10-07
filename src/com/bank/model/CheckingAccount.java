package com.bank.model;

import com.bank.exceptions.InsufficientFundsException;
import java.util.Map;

/**
 * ============================================================================
 * CHECKING / OVERDRAFT ACCOUNT
 * ============================================================================
 * Standard Overdraft-enabled account with flexible credit buffer.
 */
public class CheckingAccount extends Account {
    private static final long serialVersionUID = 1L;

    private double overdraftLimit;

    public CheckingAccount() {
        super();
        this.overdraftLimit = 25000.0;
    }

    public CheckingAccount(String accountNumber, String customerId, double initialBalance, String currency, double overdraftLimit) {
        super(accountNumber, customerId, initialBalance, currency);
        this.overdraftLimit = overdraftLimit;
    }

    @Override
    public String getAccountType() {
        return "CHECKING";
    }

    @Override
    public double calculateAnnualInterest() {
        return 0.0; // Checking / transactional accounts offer zero interest
    }

    @Override
    public void withdraw(double amount) throws InsufficientFundsException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero. Received: ₹" + amount);
        }
        getLock().lock();
        try {
            double totalAvailable = this.balance + this.overdraftLimit;
            if (amount > totalAvailable) {
                throw new InsufficientFundsException(
                    "Transaction failed: Exceeds available balance (₹" + String.format("%,.2f", balance) + 
                    ") and Overdraft limit (₹" + String.format("%,.2f", overdraftLimit) + "). Total available: ₹" + String.format("%,.2f", totalAvailable)
                );
            }
            this.balance -= amount;
        } finally {
            getLock().unlock();
        }
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = getBaseMap();
        map.put("overdraftLimit", overdraftLimit);
        map.put("availableLimit", balance + overdraftLimit);
        return map;
    }

    public double getOverdraftLimit() { return overdraftLimit; }
    public void setOverdraftLimit(double overdraftLimit) { this.overdraftLimit = overdraftLimit; }
}
