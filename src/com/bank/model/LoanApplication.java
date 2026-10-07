package com.bank.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class LoanApplication implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String customerId;
    private String customerName;
    private String loanType; // "PERSONAL", "HOME", "EDUCATION", "BUSINESS"
    private double amount;
    private int tenureMonths;
    private double interestRate;
    private double monthlyEmi;
    private String purpose;
    private String status; // "PENDING", "APPROVED", "REJECTED", "ACTIVE", "PAID_OFF"
    private String appliedAt;
    private String decidedAt;
    private String remarks;
    private double remainingPrincipal;
    private int emisPaid;

    public LoanApplication() {
        this.status = "PENDING";
        this.appliedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.emisPaid = 0;
    }

    public LoanApplication(String id, String customerId, String customerName, String loanType, double amount,
                           int tenureMonths, double interestRate, String purpose) {
        this();
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.loanType = loanType;
        this.amount = amount;
        this.tenureMonths = tenureMonths;
        this.interestRate = interestRate;
        this.purpose = purpose;
        this.monthlyEmi = calculateEmi(amount, interestRate, tenureMonths);
        this.remainingPrincipal = amount;
    }

    public static double calculateEmi(double p, double annualRate, int months) {
        if (months <= 0) return 0;
        double monthlyRate = (annualRate / 100.0) / 12.0;
        if (monthlyRate == 0) return p / months;
        double emi = (p * monthlyRate * Math.pow(1 + monthlyRate, months)) / (Math.pow(1 + monthlyRate, months) - 1);
        return Math.round(emi * 100.0) / 100.0;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("customerId", customerId);
        map.put("customerName", customerName);
        map.put("loanType", loanType);
        map.put("amount", amount);
        map.put("tenureMonths", tenureMonths);
        map.put("interestRate", interestRate);
        map.put("monthlyEmi", monthlyEmi);
        map.put("purpose", purpose);
        map.put("status", status);
        map.put("appliedAt", appliedAt);
        map.put("decidedAt", decidedAt);
        map.put("remarks", remarks);
        map.put("remainingPrincipal", remainingPrincipal);
        map.put("emisPaid", emisPaid);
        return map;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getLoanType() { return loanType; }
    public void setLoanType(String loanType) { this.loanType = loanType; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public int getTenureMonths() { return tenureMonths; }
    public void setTenureMonths(int tenureMonths) { this.tenureMonths = tenureMonths; }

    public double getInterestRate() { return interestRate; }
    public void setInterestRate(double interestRate) { this.interestRate = interestRate; }

    public double getMonthlyEmi() { return monthlyEmi; }
    public void setMonthlyEmi(double monthlyEmi) { this.monthlyEmi = monthlyEmi; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAppliedAt() { return appliedAt; }
    public void setAppliedAt(String appliedAt) { this.appliedAt = appliedAt; }

    public String getDecidedAt() { return decidedAt; }
    public void setDecidedAt(String decidedAt) { this.decidedAt = decidedAt; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public double getRemainingPrincipal() { return remainingPrincipal; }
    public void setRemainingPrincipal(double remainingPrincipal) { this.remainingPrincipal = remainingPrincipal; }

    public int getEmisPaid() { return emisPaid; }
    public void setEmisPaid(int emisPaid) { this.emisPaid = emisPaid; }
}
