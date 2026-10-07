package com.bank.dao;

import com.bank.exceptions.DatabaseException;
import com.bank.model.Account;
import java.util.List;

/**
 * ============================================================================
 * INTERFACE: AccountDAO
 * ============================================================================
 * Demonstrates:
 * - Interface Inheritance (extends GenericDAO<Account, String>)
 * - Polymorphic Account queries (SavingsAccount, CheckingAccount)
 */
public interface AccountDAO extends GenericDAO<Account, String> {

    /**
     * Retrieves all accounts owned by a specific customer.
     * @param customerId The unique identifier of the customer
     * @return List of Accounts
     * @throws DatabaseException on SQL execution failure
     */
    List<Account> findByCustomerId(String customerId) throws DatabaseException;

    /**
     * Updates an account's balance with ACID guarantees.
     * @param accountNumber Target account number
     * @param newBalance New calculated balance
     * @return true if updated successfully
     * @throws DatabaseException on SQL execution failure
     */
    boolean updateBalance(String accountNumber, double newBalance) throws DatabaseException;

    /**
     * Updates debit card details and lock/freeze status.
     * @param accountNumber Target account number
     * @param isFrozen Frozen card state
     * @return true if updated successfully
     * @throws DatabaseException on SQL execution failure
     */
    boolean updateCardStatus(String accountNumber, boolean isFrozen) throws DatabaseException;
}
