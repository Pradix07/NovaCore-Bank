package com.bank.model;

import com.bank.exceptions.InsufficientFundsException;
import java.util.Map;

/**
 * Checking Account with overdraft limit facility.
 */
public class CheckingAccount extends Account {
    private static final long serialVersionUID = 1L;

    private double overdraftLimit;

    public CheckingAccount() {
        super();
        this.overdraftLimit = 1000.0;
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
        return 0.0; // Checking accounts usually offer no or nominal interest
    }

    @Override
    public void withdraw(double amount) throws InsufficientFundsException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero.");
        }
        getLock().lock();
        try {
            if (this.balance + this.overdraftLimit < amount) {
                throw new InsufficientFundsException("Transaction failed: Exceeds available balance and overdraft limit of $" + String.format("%.2f", overdraftLimit));
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
