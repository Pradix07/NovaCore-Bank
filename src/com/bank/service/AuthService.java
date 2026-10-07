package com.bank.service;

import com.bank.exceptions.AuthenticationException;
import com.bank.exceptions.ValidationException;
import com.bank.model.*;
import com.bank.repository.DataStore;
import com.bank.util.SecurityUtil;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.bank.service.interfaces.IAuthService;

/**
 * ============================================================================
 * SERVICE: AuthService (Indian Banking System Authentication & KYC)
 * ============================================================================
 * Demonstrates:
 * - Interface Implementation (implements IAuthService)
 * - Security & Cryptography (SHA-256 Hashing, UUID session management)
 * - Indian KYC Onboarding (PAN, Aadhaar, UPI ID, IFSC Assignment)
 * - Audit logging and authentication state validation
 */
public class AuthService implements IAuthService {

    private final DataStore dataStore = DataStore.getInstance();

    public Map<String, Object> login(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new ValidationException("Username and password are required.");
        }

        User user = dataStore.getUserByUsername(username.trim());
        if (user == null) {
            throw new AuthenticationException("Invalid username or password.");
        }

        if (!user.isActive()) {
            throw new AuthenticationException("Your account is currently disabled. Please contact NovaCore Bank administration.");
        }

        if (!SecurityUtil.verifyPassword(password, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid username or password.");
        }

        user.setLastLogin(LocalDateTime.now().toString());
        String token = UUID.randomUUID().toString();
        dataStore.createSession(token, user.getId());
        dataStore.saveToFile();

        // Audit Log
        dataStore.addAuditLog(new AuditLog(
                SecurityUtil.generateId("LOG"),
                user.getId(),
                user.getFullName(),
                user.getRole(),
                "USER_LOGIN",
                "User successfully logged into " + user.getRole() + " portal at NovaCore Bank of India.",
                "127.0.0.1"
        ));

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", user.toMap());
        return response;
    }

    public Map<String, Object> registerCustomer(String username, String password, String fullName, String email, String phone, String address, String panOrTaxId, String initialAccountType, double initialDeposit) {
        return registerCustomerDetailed(username, password, fullName, email, phone, address, panOrTaxId, "XXXX-XXXX-1234", initialAccountType, initialDeposit, null, null);
    }

    /**
     * Detailed customer registration with Indian KYC & Account specification.
     */
    public Map<String, Object> registerCustomerDetailed(String username, String password, String fullName, String email, String phone, String address, String panOrTaxId, String aadhaarNumber, String initialAccountType, double initialDeposit, String extra1, String extra2) {
        if (username == null || username.trim().length() < 3) {
            throw new ValidationException("Username must be at least 3 characters long.");
        }
        if (password == null || password.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters long.");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new ValidationException("Full name is required.");
        }
        if (email == null || !email.contains("@")) {
            throw new ValidationException("A valid email address is required.");
        }

        if (dataStore.getUserByUsername(username.trim()) != null) {
            throw new ValidationException("Username '" + username + "' is already registered with NovaCore Bank.");
        }

        String custId = SecurityUtil.generateId("USR-CUST");
        String passwordHash = SecurityUtil.hashPassword(password);
        String pan = (panOrTaxId != null && !panOrTaxId.trim().isEmpty()) ? panOrTaxId.trim().toUpperCase() : "AAAPA1234B";
        String aadhaar = (aadhaarNumber != null && !aadhaarNumber.trim().isEmpty()) ? aadhaarNumber.trim() : "XXXX-XXXX-9876";
        String upi = username.trim().toLowerCase() + "@novabank";

        Customer customer = new Customer(custId, username.trim(), passwordHash, fullName.trim(), email.trim(), phone != null ? phone.trim() : "+91-9876543210", address != null ? address.trim() : "Mumbai, Maharashtra", pan, aadhaar, upi, "1234");

        // Determine Account Type (SAVINGS, CURRENT, STUDENT)
        String accType = (initialAccountType != null) ? initialAccountType.toUpperCase().trim() : "SAVINGS";
        double minDeposit = "STUDENT".equals(accType) ? 100.0 : ("CURRENT".equals(accType) ? 5000.0 : 1000.0);
        double deposit = Math.max(initialDeposit, minDeposit);

        String accNum = SecurityUtil.generateAccountNumber(accType);
        Account account;

        if ("CURRENT".equals(accType)) {
            String tradeLicenseOrGst = (extra1 != null && !extra1.trim().isEmpty()) ? extra1.trim() : "27AAAPA1234B1Z5";
            String businessName = (extra2 != null && !extra2.trim().isEmpty()) ? extra2.trim() : fullName + " Enterprises";
            account = new CurrentAccount(accNum, custId, deposit, "INR", tradeLicenseOrGst, businessName, 50000.0, 5000.0);
            account.setBranchName("Mumbai Fort Main Branch");
            account.setIfscCode("NOVA0001001");
        } else if ("STUDENT".equals(accType)) {
            String inst = (extra1 != null && !extra1.trim().isEmpty()) ? extra1.trim() : "Educational University";
            String stuId = (extra2 != null && !extra2.trim().isEmpty()) ? extra2.trim() : "STU-" + ((int)(Math.random() * 9000) + 1000);
            account = new StudentAccount(accNum, custId, deposit, "INR", inst, stuId, 0.035, 100.0, 100000.0, 20000.0);
            account.setBranchName("New Delhi Connaught Place Branch");
            account.setIfscCode("NOVA0003003");
        } else {
            accType = "SAVINGS";
            account = new SavingsAccount(accNum, custId, deposit, "INR", dataStore.getSystemSettings().getDefaultSavingsInterestRate(), dataStore.getSystemSettings().getMinSavingsBalance(), 50000.0);
            account.setBranchName("Mumbai Fort Main Branch");
            account.setIfscCode("NOVA0001001");
        }

        account.setCardNumber(SecurityUtil.generateCardNumber());
        account.setCardExpiry(SecurityUtil.generateCardExpiry());
        account.setCardCvv(SecurityUtil.generateCvv());
        account.setUpiId(upi);

        customer.addAccountNumber(account.getAccountNumber());

        dataStore.addUser(customer);
        dataStore.addAccount(account);

        // Initial deposit transaction
        Transaction initialTx = new Transaction(
                SecurityUtil.generateId("TXN"),
                TransactionType.DEPOSIT,
                deposit,
                "INITIAL_OPENING_DEPOSIT",
                accNum,
                "Account Opening Initial Deposit",
                deposit,
                custId,
                SecurityUtil.generateReferenceNumber()
        );
        dataStore.addTransaction(initialTx);

        // Audit Log
        dataStore.addAuditLog(new AuditLog(
                SecurityUtil.generateId("LOG"),
                custId,
                fullName,
                "CUSTOMER",
                "ACCOUNT_REGISTERED",
                "New customer onboarded with initial " + accType + " account #" + accNum + " (₹" + String.format("%,.2f", deposit) + ", IFSC: " + account.getIfscCode() + ", PAN: " + pan + ")",
                "127.0.0.1"
        ));

        dataStore.saveToFile();

        String token = UUID.randomUUID().toString();
        dataStore.createSession(token, custId);

        Map<String, Object> res = new HashMap<>();
        res.put("token", token);
        res.put("user", customer.toMap());
        res.put("initialAccount", account.toMap());
        return res;
    }

    public User authenticateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new AuthenticationException("Session token is missing. Please log in.");
        }
        String userId = dataStore.getUserIdByToken(token);
        if (userId == null) {
            throw new AuthenticationException("Session expired or invalid token. Please log in again.");
        }
        User user = dataStore.getUserById(userId);
        if (user == null || !user.isActive()) {
            throw new AuthenticationException("User account is inactive or not found.");
        }
        return user;
    }

    public void logout(String token) {
        if (token != null) {
            dataStore.removeSession(token);
        }
    }
}
