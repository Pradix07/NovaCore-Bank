package com.bank.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class Investment implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String customerId;
    private String type; // "FIXED_DEPOSIT", "MUTUAL_FUND", "GOLD_BOND"
    private String name;
    private double principalAmount;
    private double interestRate; // Annual %
    private int durationMonths;
    private double expectedReturn;
    private double currentMaturityValue;
    private String startDate;
    private String maturityDate;
    private String status; // "ACTIVE", "MATURED", "WITHDRAWN"

    public Investment() {
        this.status = "ACTIVE";
        this.startDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public Investment(String id, String customerId, String type, String name, double principalAmount,
                      double interestRate, int durationMonths) {
        this();
        this.id = id;
        this.customerId = customerId;
        this.type = type;
        this.name = name;
        this.principalAmount = principalAmount;
        this.interestRate = interestRate;
        this.durationMonths = durationMonths;
        this.expectedReturn = (principalAmount * (interestRate / 100.0) * (durationMonths / 12.0));
        this.currentMaturityValue = Math.round((principalAmount + expectedReturn) * 100.0) / 100.0;
        this.maturityDate = LocalDateTime.now().plusMonths(durationMonths).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("customerId", customerId);
        map.put("type", type);
        map.put("name", name);
        map.put("principalAmount", principalAmount);
        map.put("interestRate", interestRate);
        map.put("durationMonths", durationMonths);
        map.put("expectedReturn", expectedReturn);
        map.put("currentMaturityValue", currentMaturityValue);
        map.put("startDate", startDate);
        map.put("maturityDate", maturityDate);
        map.put("status", status);
        return map;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(double principalAmount) { this.principalAmount = principalAmount; }

    public double getInterestRate() { return interestRate; }
    public void setInterestRate(double interestRate) { this.interestRate = interestRate; }

    public int getDurationMonths() { return durationMonths; }
    public void setDurationMonths(int durationMonths) { this.durationMonths = durationMonths; }

    public double getExpectedReturn() { return expectedReturn; }
    public void setExpectedReturn(double expectedReturn) { this.expectedReturn = expectedReturn; }

    public double getCurrentMaturityValue() { return currentMaturityValue; }
    public void setCurrentMaturityValue(double currentMaturityValue) { this.currentMaturityValue = currentMaturityValue; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getMaturityDate() { return maturityDate; }
    public void setMaturityDate(String maturityDate) { this.maturityDate = maturityDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
