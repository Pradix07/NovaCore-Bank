package com.bank.model;

import com.bank.exceptions.InsufficientFundsException;
import com.bank.exceptions.MaxWithdrawLimitException;
import java.util.Map;

/**
 * ============================================================================
 * SAVINGS ACCOUNT (Indian Banking System)
 * ============================================================================
 * Features:
 * - Minimum balance requirement (RBI standard ₹1,000.00)
 * - Annual interest rate (4.0% p.a.)
 * - Daily maximum withdrawal limit (₹50,000.00)
 * - Instant RuPay / Visa Debit card integration
 */
public class SavingsAccount extends Account {
    private static final long serialVersionUID = 1L;

    private double interestRate;      // e.g. 0.040 for 4.0% p.a.
    private double minimumBalance;     // Minimum balance required: ₹1,000
    private double maxWithdrawLimit;   // Max per-transaction / daily limit: ₹50,000

    public SavingsAccount() {
        super();
        this.interestRate = 0.040;
        this.minimumBalance = 1000.0;
        this.maxWithdrawLimit = 50000.0;
    }

    public SavingsAccount(String accountNumber, String customerId, double initialBalance, String currency, double interestRate, double minimumBalance) {
        super(accountNumber, customerId, initialBalance, currency);
        this.interestRate = interestRate;
        this.minimumBalance = minimumBalance;
        this.maxWithdrawLimit = 50000.0;
    }

    public SavingsAccount(String accountNumber, String customerId, double initialBalance, String currency, double interestRate, double minimumBalance, double maxWithdrawLimit) {
        super(accountNumber, customerId, initialBalance, currency);
        this.interestRate = interestRate;
        this.minimumBalance = minimumBalance;
        this.maxWithdrawLimit = maxWithdrawLimit;
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
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero. Received: ₹" + amount);
        }
        if (maxWithdrawLimit > 0 && amount > maxWithdrawLimit) {
            throw new MaxWithdrawLimitException("Withdrawal exceeds maximum per-transaction limit of ₹" + String.format("%,.2f", maxWithdrawLimit));
        }
        getLock().lock();
        try {
            if (this.balance - amount < this.minimumBalance) {
                throw new InsufficientFundsException(
                    "Transaction failed: Minimum balance of ₹" + String.format("%,.2f", minimumBalance) + 
                    " must be maintained in this Savings Account. Current balance: ₹" + String.format("%,.2f", balance)
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
        map.put("interestRate", interestRate);
        map.put("minimumBalance", minimumBalance);
        map.put("maxWithdrawLimit", maxWithdrawLimit);
        map.put("projectedAnnualInterest", calculateAnnualInterest());
        return map;
    }

    public double getInterestRate() { return interestRate; }
    public void setInterestRate(double interestRate) { this.interestRate = interestRate; }

    public double getMinimumBalance() { return minimumBalance; }
    public void setMinimumBalance(double minimumBalance) { this.minimumBalance = minimumBalance; }

    public double getMaxWithdrawLimit() { return maxWithdrawLimit; }
    public void setMaxWithdrawLimit(double maxWithdrawLimit) { this.maxWithdrawLimit = maxWithdrawLimit; }
}
