package com.bank.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * CUSTOMER MODEL (Indian Banking System)
 * ============================================================================
 * Encapsulates retail & commercial banking customer profile:
 * - KYC Verification: PAN (Permanent Account Number), Aadhaar Number
 * - Digital Payments: UPI ID (e.g., name@novabank)
 * - Security: 4-digit Transaction / ATM PIN
 * - Associated Indian Bank Account Numbers
 */
public class Customer extends User {
    private static final long serialVersionUID = 1L;

    private String address;
    private String panOrTaxId;    // PAN Number (e.g., "AAAPA1234B")
    private String aadhaarNumber;  // Masked/12-digit Aadhaar (e.g., "XXXX-XXXX-9876")
    private String upiId;          // UPI Handle (e.g., "aarav.patel@novabank")
    private String securityPin;    // 4-digit Transaction / ATM PIN
    private List<String> accountNumbers;
    private String occupation;
    private double monthlyIncome;

    public Customer() {
        super();
        this.role = "CUSTOMER";
        this.accountNumbers = new ArrayList<>();
    }

    public Customer(String id, String username, String passwordHash, String fullName, String email, String phone,
                    String address, String panOrTaxId, String securityPin) {
        super(id, username, passwordHash, fullName, email, phone, "CUSTOMER");
        this.address = address;
        this.panOrTaxId = panOrTaxId;
        this.aadhaarNumber = "XXXX-XXXX-1234";
        this.upiId = username + "@novabank";
        this.securityPin = securityPin;
        this.accountNumbers = new ArrayList<>();
        this.occupation = "Professional";
        this.monthlyIncome = 50000.0;
    }

    public Customer(String id, String username, String passwordHash, String fullName, String email, String phone,
                    String address, String panOrTaxId, String aadhaarNumber, String upiId, String securityPin) {
        super(id, username, passwordHash, fullName, email, phone, "CUSTOMER");
        this.address = address;
        this.panOrTaxId = panOrTaxId;
        this.aadhaarNumber = aadhaarNumber;
        this.upiId = (upiId != null && !upiId.isEmpty()) ? upiId : (username + "@novabank");
        this.securityPin = securityPin;
        this.accountNumbers = new ArrayList<>();
        this.occupation = "Professional";
        this.monthlyIncome = 50000.0;
    }

    @Override
    public String getDashboardType() {
        return "CUSTOMER_DASHBOARD";
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = getBaseMap();
        map.put("address", address);
        map.put("panOrTaxId", panOrTaxId);
        map.put("aadhaarNumber", aadhaarNumber);
        map.put("upiId", upiId != null ? upiId : (username + "@novabank"));
        map.put("accountNumbers", accountNumbers);
        map.put("occupation", occupation);
        map.put("monthlyIncome", monthlyIncome);
        return map;
    }

    // Getters and Setters
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPanOrTaxId() { return panOrTaxId; }
    public void setPanOrTaxId(String panOrTaxId) { this.panOrTaxId = panOrTaxId; }

    public String getAadhaarNumber() { return aadhaarNumber; }
    public void setAadhaarNumber(String aadhaarNumber) { this.aadhaarNumber = aadhaarNumber; }

    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }

    public String getSecurityPin() { return securityPin; }
    public void setSecurityPin(String securityPin) { this.securityPin = securityPin; }

    public List<String> getAccountNumbers() { return accountNumbers; }
    public void setAccountNumbers(List<String> accountNumbers) { this.accountNumbers = accountNumbers; }

    public void addAccountNumber(String accountNumber) {
        if (!this.accountNumbers.contains(accountNumber)) {
            this.accountNumbers.add(accountNumber);
        }
    }

    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }

    public double getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(double monthlyIncome) { this.monthlyIncome = monthlyIncome; }
}
