package com.bank.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String timestamp;
    private TransactionType type;
    private double amount;
    private String fromAccount;
    private String toAccount;
    private String description;
    private double balanceAfter;
    private String status; // "SUCCESS", "FAILED", "PENDING"
    private String customerId;
    private String referenceNumber;

    public Transaction() {
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.status = "SUCCESS";
    }

    public Transaction(String id, TransactionType type, double amount, String fromAccount, String toAccount,
                       String description, double balanceAfter, String customerId, String referenceNumber) {
        this();
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.description = description;
        this.balanceAfter = balanceAfter;
        this.customerId = customerId;
        this.referenceNumber = referenceNumber;
        this.status = "SUCCESS";
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("timestamp", timestamp);
        map.put("type", type != null ? type.name() : "");
        map.put("amount", amount);
        map.put("fromAccount", fromAccount);
        map.put("toAccount", toAccount);
        map.put("description", description);
        map.put("balanceAfter", balanceAfter);
        map.put("status", status);
        map.put("customerId", customerId);
        map.put("referenceNumber", referenceNumber);
        return map;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getFromAccount() { return fromAccount; }
    public void setFromAccount(String fromAccount) { this.fromAccount = fromAccount; }

    public String getToAccount() { return toAccount; }
    public void setToAccount(String toAccount) { this.toAccount = toAccount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(double balanceAfter) { this.balanceAfter = balanceAfter; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
}
