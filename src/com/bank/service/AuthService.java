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
 * SERVICE: AuthService
 * ============================================================================
 * Demonstrates:
 * - Interface Implementation (implements IAuthService)
 * - Security & Cryptography (SHA-256 Hashing, UUID session management)
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
            throw new AuthenticationException("Your account is currently disabled. Please contact bank administration.");
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
                "User successfully logged in into " + user.getRole() + " portal.",
                "127.0.0.1"
        ));

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", user.toMap());
        return response;
    }

    public Map<String, Object> registerCustomer(String username, String password, String fullName, String email, String phone, String address, String panOrTaxId, String initialAccountType, double initialDeposit) {
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
            throw new ValidationException("Username '" + username + "' is already taken.");
        }

        String custId = SecurityUtil.generateId("USR-CUST");
        String passwordHash = SecurityUtil.hashPassword(password);
        Customer customer = new Customer(custId, username.trim(), passwordHash, fullName.trim(), email.trim(), phone != null ? phone.trim() : "", address != null ? address.trim() : "", panOrTaxId != null ? panOrTaxId.trim() : "", "1234");

        // Create default account
        String accType = (initialAccountType != null && initialAccountType.equalsIgnoreCase("CHECKING")) ? "CHECKING" : "SAVINGS";
        String accNum = SecurityUtil.generateAccountNumber(accType);
        double deposit = Math.max(initialDeposit, 100.0);

        Account account;
        if ("SAVINGS".equals(accType)) {
            account = new SavingsAccount(accNum, custId, deposit, "INR", dataStore.getSystemSettings().getDefaultSavingsInterestRate(), dataStore.getSystemSettings().getMinSavingsBalance());
        } else {
            account = new CheckingAccount(accNum, custId, deposit, "INR", 1000.0);
        }

        account.setCardNumber(SecurityUtil.generateCardNumber());
        account.setCardExpiry(SecurityUtil.generateCardExpiry());
        account.setCardCvv(SecurityUtil.generateCvv());

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
                "New customer registered with initial account #" + accNum + " ($" + deposit + ")",
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
