package com.bank.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SystemSettings implements Serializable {
    private static final long serialVersionUID = 1L;

    private String bankName;
    private double defaultSavingsInterestRate; // e.g. 4.5
    private double defaultLoanInterestRate;    // e.g. 8.5
    private double defaultFdInterestRate;      // e.g. 7.0
    private double dailyTransferLimit;         // e.g. 25000.0
    private double perTransactionLimit;        // e.g. 10000.0
    private double minSavingsBalance;          // e.g. 500.0
    private double transactionFeePercent;      // e.g. 0.0 or 0.25
    private boolean maintenanceMode;
    private String supportEmail;
    private String supportPhone;
    private String currencySymbol;

    public SystemSettings() {
        this.bankName = "NovaCore Global Bank";
        this.defaultSavingsInterestRate = 4.5;
        this.defaultLoanInterestRate = 8.75;
        this.defaultFdInterestRate = 7.25;
        this.dailyTransferLimit = 50000.0;
        this.perTransactionLimit = 20000.0;
        this.minSavingsBalance = 500.0;
        this.transactionFeePercent = 0.0;
        this.maintenanceMode = false;
        this.supportEmail = "support@novacorebank.com";
        this.supportPhone = "+91 1800 555 NOVA";
        this.currencySymbol = "₹";
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("bankName", bankName);
        map.put("defaultSavingsInterestRate", defaultSavingsInterestRate);
        map.put("defaultLoanInterestRate", defaultLoanInterestRate);
        map.put("defaultFdInterestRate", defaultFdInterestRate);
        map.put("dailyTransferLimit", dailyTransferLimit);
        map.put("perTransactionLimit", perTransactionLimit);
        map.put("minSavingsBalance", minSavingsBalance);
        map.put("transactionFeePercent", transactionFeePercent);
        map.put("maintenanceMode", maintenanceMode);
        map.put("supportEmail", supportEmail);
        map.put("supportPhone", supportPhone);
        map.put("currencySymbol", currencySymbol);
        return map;
    }

    // Getters and Setters
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public double getDefaultSavingsInterestRate() { return defaultSavingsInterestRate; }
    public void setDefaultSavingsInterestRate(double defaultSavingsInterestRate) { this.defaultSavingsInterestRate = defaultSavingsInterestRate; }

    public double getDefaultLoanInterestRate() { return defaultLoanInterestRate; }
    public void setDefaultLoanInterestRate(double defaultLoanInterestRate) { this.defaultLoanInterestRate = defaultLoanInterestRate; }

    public double getDefaultFdInterestRate() { return defaultFdInterestRate; }
    public void setDefaultFdInterestRate(double defaultFdInterestRate) { this.defaultFdInterestRate = defaultFdInterestRate; }

    public double getDailyTransferLimit() { return dailyTransferLimit; }
    public void setDailyTransferLimit(double dailyTransferLimit) { this.dailyTransferLimit = dailyTransferLimit; }

    public double getPerTransactionLimit() { return perTransactionLimit; }
    public void setPerTransactionLimit(double perTransactionLimit) { this.perTransactionLimit = perTransactionLimit; }

    public double getMinSavingsBalance() { return minSavingsBalance; }
    public void setMinSavingsBalance(double minSavingsBalance) { this.minSavingsBalance = minSavingsBalance; }

    public double getTransactionFeePercent() { return transactionFeePercent; }
    public void setTransactionFeePercent(double transactionFeePercent) { this.transactionFeePercent = transactionFeePercent; }

    public boolean isMaintenanceMode() { return maintenanceMode; }
    public void setMaintenanceMode(boolean maintenanceMode) { this.maintenanceMode = maintenanceMode; }

    public String getSupportEmail() { return supportEmail; }
    public void setSupportEmail(String supportEmail) { this.supportEmail = supportEmail; }

    public String getSupportPhone() { return supportPhone; }
    public void setSupportPhone(String supportPhone) { this.supportPhone = supportPhone; }

    public String getCurrencySymbol() { return currencySymbol; }
    public void setCurrencySymbol(String currencySymbol) { this.currencySymbol = currencySymbol; }
}
