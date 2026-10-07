package com.bank.model;

import com.bank.exceptions.InsufficientFundsException;
import java.util.Map;

/**
 * Savings Account with minimum balance requirement and compounding interest.
 */
public class SavingsAccount extends Account {
    private static final long serialVersionUID = 1L;

    private double interestRate; // e.g. 0.045 for 4.5%
    private double minimumBalance;

    public SavingsAccount() {
        super();
        this.interestRate = 0.045;
        this.minimumBalance = 500.0;
    }

    public SavingsAccount(String accountNumber, String customerId, double initialBalance, String currency, double interestRate, double minimumBalance) {
        super(accountNumber, customerId, initialBalance, currency);
        this.interestRate = interestRate;
        this.minimumBalance = minimumBalance;
    }

    @Override
    public String getAccountType() {
        return "SAVINGS";
    }

    @Override
    public double calculateAnnualInterest() {
        return this.balance * this.interestRate;
    }

    @Override
    public void withdraw(double amount) throws InsufficientFundsException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero.");
        }
        getLock().lock();
        try {
            if (this.balance - amount < this.minimumBalance) {
                throw new InsufficientFundsException("Transaction failed: Minimum balance of $" + String.format("%.2f", minimumBalance) + " must be maintained.");
            }
            this.balance -= amount;
        } finally {
            getLock().unlock();
        }
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = getBaseMap();
        map.put("interestRate", interestRate);
        map.put("minimumBalance", minimumBalance);
        map.put("projectedAnnualInterest", calculateAnnualInterest());
        return map;
    }

    public double getInterestRate() { return interestRate; }
    public void setInterestRate(double interestRate) { this.interestRate = interestRate; }

    public double getMinimumBalance() { return minimumBalance; }
    public void setMinimumBalance(double minimumBalance) { this.minimumBalance = minimumBalance; }
}
