package com.bank.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Customer model class extending User.
 */
public class Customer extends User {
    private static final long serialVersionUID = 1L;

    private String address;
    private String panOrTaxId;
    private String securityPin;
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
        map.put("accountNumbers", accountNumbers);
        map.put("occupation", occupation);
        map.put("monthlyIncome", monthlyIncome);
        return map;
    }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPanOrTaxId() { return panOrTaxId; }
    public void setPanOrTaxId(String panOrTaxId) { this.panOrTaxId = panOrTaxId; }

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
