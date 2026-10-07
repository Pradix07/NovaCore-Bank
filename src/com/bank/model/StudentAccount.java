package com.bank.model;

import com.bank.exceptions.InsufficientFundsException;
import com.bank.exceptions.MaxBalanceLimitException;
import com.bank.exceptions.MaxWithdrawLimitException;
import java.util.Map;

/**
 * ============================================================================
 * STUDENT ACCOUNT (Indian University / College Student Banking)
 * ============================================================================
 * Features:
 * - Special Zero-Balance / Low-Balance Account (Min balance: ₹100.00 / ₹0.00)
 * - Institution Name & Student Roll ID verification
 * - Protective maximum balance cap (e.g. ₹1,00,000.00)
 * - Annual interest rate (3.5% p.a.)
 * - Free student RuPay Debit Card with zero AMC
 */
public class StudentAccount extends Account {
    private static final long serialVersionUID = 1L;

    private String institutionName;     // e.g. "Indian Institute of Technology (IIT) Bombay"
    private String studentId;           // e.g. "IITB-2026-CS104"
    private double interestRate;        // e.g. 0.035 for 3.5% p.a.
    private double minimumBalance;       // Nominal minimum balance: ₹100.00
    private double maxBalanceLimit;     // Protective balance limit: ₹1,00,000.00
    private double maxWithdrawLimit;    // Daily withdrawal limit: ₹20,000.00

    public StudentAccount() {
        super();
        this.institutionName = "Educational Institution";
        this.studentId = "STU-0000";
        this.interestRate = 0.035;
        this.minimumBalance = 100.0;
        this.maxBalanceLimit = 100000.0;
        this.maxWithdrawLimit = 20000.0;
    }

    public StudentAccount(String accountNumber, String customerId, double initialBalance, String currency,
                          String institutionName, String studentId) {
        super(accountNumber, customerId, initialBalance, currency);
        this.institutionName = institutionName;
        this.studentId = studentId;
        this.interestRate = 0.035;
        this.minimumBalance = 100.0;
        this.maxBalanceLimit = 100000.0;
        this.maxWithdrawLimit = 20000.0;
    }

    public StudentAccount(String accountNumber, String customerId, double initialBalance, String currency,
                          String institutionName, String studentId, double interestRate, double minimumBalance,
                          double maxBalanceLimit, double maxWithdrawLimit) {
        super(accountNumber, customerId, initialBalance, currency);
        this.institutionName = institutionName;
        this.studentId = studentId;
        this.interestRate = interestRate;
        this.minimumBalance = minimumBalance;
        this.maxBalanceLimit = maxBalanceLimit;
        this.maxWithdrawLimit = maxWithdrawLimit;
    }

    @Override
    public String getAccountType() {
        return "STUDENT";
    }

    @Override
    public double calculateAnnualInterest() {
        return this.balance * this.interestRate;
    }

    @Override
    public void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero. Received: ₹" + amount);
        }
        if (this.balance + amount > this.maxBalanceLimit) {
            throw new MaxBalanceLimitException(
                "Deposit exceeds Student Account maximum balance cap of ₹" + String.format("%,.2f", maxBalanceLimit) + 
                ". Current balance: ₹" + String.format("%,.2f", balance) + ". Consider upgrading to a standard Savings Account."
            );
        }
        super.deposit(amount);
    }

    @Override
    public void withdraw(double amount) throws InsufficientFundsException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero. Received: ₹" + amount);
        }
        if (maxWithdrawLimit > 0 && amount > maxWithdrawLimit) {
            throw new MaxWithdrawLimitException("Withdrawal exceeds Student Account per-transaction limit of ₹" + String.format("%,.2f", maxWithdrawLimit));
        }
        getLock().lock();
        try {
            if (this.balance - amount < this.minimumBalance) {
                throw new InsufficientFundsException(
                    "Transaction failed: Minimum balance of ₹" + String.format("%,.2f", minimumBalance) + 
                    " must be maintained in Student Account. Current balance: ₹" + String.format("%,.2f", balance)
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
        map.put("institutionName", institutionName);
        map.put("studentId", studentId);
        map.put("interestRate", interestRate);
        map.put("minimumBalance", minimumBalance);
        map.put("maxBalanceLimit", maxBalanceLimit);
        map.put("maxWithdrawLimit", maxWithdrawLimit);
        map.put("projectedAnnualInterest", calculateAnnualInterest());
        return map;
    }

    // Getters and Setters
    public String getInstitutionName() { return institutionName; }
    public void setInstitutionName(String institutionName) { this.institutionName = institutionName; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public double getInterestRate() { return interestRate; }
    public void setInterestRate(double interestRate) { this.interestRate = interestRate; }

    public double getMinimumBalance() { return minimumBalance; }
    public void setMinimumBalance(double minimumBalance) { this.minimumBalance = minimumBalance; }

    public double getMaxBalanceLimit() { return maxBalanceLimit; }
    public void setMaxBalanceLimit(double maxBalanceLimit) { this.maxBalanceLimit = maxBalanceLimit; }

    public double getMaxWithdrawLimit() { return maxWithdrawLimit; }
    public void setMaxWithdrawLimit(double maxWithdrawLimit) { this.maxWithdrawLimit = maxWithdrawLimit; }
}
