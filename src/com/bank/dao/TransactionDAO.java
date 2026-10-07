package com.bank.dao;

import com.bank.exceptions.DatabaseException;
import com.bank.model.Transaction;
import java.util.List;

/**
 * ============================================================================
 * INTERFACE: TransactionDAO
 * ============================================================================
 * Demonstrates:
 * - Data Access Object pattern for financial transaction history
 * - Filtering and pagination operations via SQL
 */
public interface TransactionDAO extends GenericDAO<Transaction, String> {

    /**
     * Finds transactions associated with a particular account number.
     * @param accountNumber The account number (source or destination)
     * @param limit Max records to retrieve
     * @return List of transactions sorted newest first
     * @throws DatabaseException on SQL execution failure
     */
    List<Transaction> findByAccountNumber(String accountNumber, int limit) throws DatabaseException;

    /**
     * Finds all transactions for a specific customer.
     * @param customerId The customer ID
     * @return List of transactions
     * @throws DatabaseException on SQL execution failure
     */
    List<Transaction> findByCustomerId(String customerId) throws DatabaseException;
}
