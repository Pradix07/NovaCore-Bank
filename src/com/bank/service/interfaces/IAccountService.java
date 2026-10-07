package com.bank.service.interfaces;

import com.bank.exceptions.AccountNotFoundException;
import com.bank.exceptions.ValidationException;
import com.bank.model.Account;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * INTERFACE: IAccountService
 * ============================================================================
 * Demonstrates:
 * - Interface Segregation for Bank Account management
 */
public interface IAccountService {

    /**
     * Aggregates customer balances, cards, loans, and recent transactions for dashboard.
     * @param customerId Customer ID
     * @return Aggregated dashboard data map
     */
    Map<String, Object> getCustomerDashboardOverview(String customerId);

    /**
     * Retrieves all accounts belonging to a customer.
     * @param customerId The customer ID
     * @return List of accounts
     */
    List<Account> getCustomerAccounts(String customerId);

    /**
     * Retrieves account by account number.
     * @param accountNumber The account number
     * @return Account entity
     * @throws AccountNotFoundException if not found
     */
    Account getAccount(String accountNumber) throws AccountNotFoundException;

    /**
     * Opens an additional savings or checking account for an existing customer.
     * @param customerId Customer ID
     * @param accountType SAVINGS or CHECKING
     * @param initialDeposit Initial funding amount
     * @return Created Account instance
     */
    Account openNewAccount(String customerId, String accountType, double initialDeposit);

    /**
     * Toggles lock/freeze status on a debit card.
     * @param customerId The customer ID
     * @param accountNumber The account number
     * @return New frozen status (true = frozen, false = active)
     */
    boolean toggleCardFreeze(String customerId, String accountNumber);

    /**
     * Updates customer profile and transaction PIN.
     * @param customerId Customer ID
     * @param fullName Full name
     * @param email Email address
     * @param phone Phone number
     * @param address Residential address
     * @param currentPassword Current password for verification
     * @param newPassword New password
     * @param securityPin New 4-digit PIN
     * @throws ValidationException on validation failure
     */
    void updateCustomerProfile(String customerId, String fullName, String email, String phone, 
                               String address, String currentPassword, String newPassword, String securityPin) 
            throws ValidationException;
}
