package com.bank.model;

import com.bank.exceptions.InsufficientFundsException;
import com.bank.exceptions.MaxWithdrawLimitException;
import java.util.Map;

/**
 * ============================================================================
 * CURRENT ACCOUNT (Indian Commercial / Business Banking)
 * ============================================================================
 * Features:
 * - Trade License Number / GSTIN verification
 * - Overdraft facility for business cash-flow operations
 * - Minimum operational balance (₹5,000.00 standard)
 * - Higher transaction volume and limits
 */
public class CurrentAccount extends Account {
    private static final long serialVersionUID = 1L;

    private String tradeLicenseOrGst; // Indian GSTIN (e.g., "27AAAPL1234C1Z5") or Trade License
    private String businessName;       // Registered enterprise / commercial entity name
    private double overdraftLimit;     // Allowed overdraft credit buffer (e.g., ₹1,00,000.00)
    private double minimumBalance;     // Minimum balance required (e.g., ₹5,000.00)

    public CurrentAccount() {
        super();
        this.overdraftLimit = 50000.0;
        this.minimumBalance = 5000.0;
        this.tradeLicenseOrGst = "GSTIN-PENDING";
        this.businessName = "Commercial Enterprise";
    }

    public CurrentAccount(String accountNumber, String customerId, double initialBalance, String currency, String tradeLicenseOrGst) {
        super(accountNumber, customerId, initialBalance, currency);
        this.tradeLicenseOrGst = tradeLicenseOrGst;
        this.overdraftLimit = 50000.0;
        this.minimumBalance = 5000.0;
        this.businessName = "Commercial Enterprise";
    }

    public CurrentAccount(String accountNumber, String customerId, double initialBalance, String currency,
                          String tradeLicenseOrGst, String businessName, double overdraftLimit, double minimumBalance) {
        super(accountNumber, customerId, initialBalance, currency);
        this.tradeLicenseOrGst = tradeLicenseOrGst;
        this.businessName = businessName;
        this.overdraftLimit = overdraftLimit;
        this.minimumBalance = minimumBalance;
    }

    @Override
    public String getAccountType() {
        return "CURRENT";
    }

    @Override
    public double calculateAnnualInterest() {
        // RBI Guidelines: Standard commercial current accounts do not earn interest
        return 0.0;
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
        map.put("tradeLicenseOrGst", tradeLicenseOrGst);
        map.put("businessName", businessName);
        map.put("overdraftLimit", overdraftLimit);
        map.put("minimumBalance", minimumBalance);
        map.put("availableBalanceWithOverdraft", balance + overdraftLimit);
        return map;
    }

    // Getters and Setters
    public String getTradeLicenseOrGst() { return tradeLicenseOrGst; }
    public void setTradeLicenseOrGst(String tradeLicenseOrGst) { this.tradeLicenseOrGst = tradeLicenseOrGst; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public double getOverdraftLimit() { return overdraftLimit; }
    public void setOverdraftLimit(double overdraftLimit) { this.overdraftLimit = overdraftLimit; }

    public double getMinimumBalance() { return minimumBalance; }
    public void setMinimumBalance(double minimumBalance) { this.minimumBalance = minimumBalance; }
}
