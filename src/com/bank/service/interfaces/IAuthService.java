package com.bank.service.interfaces;

import com.bank.exceptions.AuthenticationException;
import com.bank.exceptions.ValidationException;
import com.bank.model.User;
import java.util.Map;

/**
 * ============================================================================
 * INTERFACE: IAuthService
 * ============================================================================
 * Demonstrates:
 * - Interface Segregation Principle (ISP)
 * - Abstraction of authentication, token generation, and user validation
 */
public interface IAuthService {

    /**
     * Authenticates a user and generates a session token.
     * @param username Login username
     * @param password Plain-text password
     * @return Authentication payload with token and user details
     * @throws AuthenticationException on credentials failure
     */
    Map<String, Object> login(String username, String password) throws AuthenticationException;

    /**
     * Registers a new customer into the banking system.
     * @param username Desired username
     * @param password Account password
     * @param fullName Full legal name
     * @param email Valid email address
     * @param phone Contact number
     * @param address Residential address
     * @param panOrTaxId Tax / PAN ID
     * @param initialAccountType SAVINGS or CHECKING
     * @param initialDeposit Starting balance
     * @return New customer details and auto-generated account numbers
     * @throws ValidationException on invalid input data
     */
    Map<String, Object> registerCustomer(String username, String password, String fullName, String email, 
                                          String phone, String address, String panOrTaxId, 
                                          String initialAccountType, double initialDeposit) throws ValidationException;

    /**
     * Resolves and authenticates a user session from their bearer token.
     * @param token Active session token
     * @return Authenticated User entity
     * @throws AuthenticationException if invalid or expired
     */
    User authenticateToken(String token) throws AuthenticationException;

    /**
     * Terminates a user session.
     * @param token Active session token
     */
    void logout(String token);
}
