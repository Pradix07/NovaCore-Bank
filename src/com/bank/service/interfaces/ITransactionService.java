package com.bank.service.interfaces;

import com.bank.exceptions.BankingException;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * INTERFACE: ITransactionService
 * ============================================================================
 * Demonstrates:
 * - Interface Segregation for Financial Transactions
 * - Multithreading & Concurrent Transaction Processing with ReentrantLock
 */
public interface ITransactionService {

    /**
     * Executes a deposit into an account.
     * @param customerId Customer ID
     * @param accountNumber Target account number
     * @param amount Monetary amount
     * @param description Transaction remarks
     * @return Operation result map
     */
    Map<String, Object> deposit(String customerId, String accountNumber, double amount, String description);

    /**
     * Executes a withdrawal from an account.
     * @param customerId Customer ID
     * @param accountNumber Source account number
     * @param amount Monetary amount
     * @param description Transaction remarks
     * @return Operation result map
     */
    Map<String, Object> withdraw(String customerId, String accountNumber, double amount, String description);

    /**
     * Executes an atomic, thread-safe transfer between two accounts with deadlock avoidance.
     * @param customerId Sender customer ID
     * @param fromAccNumber Source account number
     * @param toAccNumber Destination account number
     * @param amount Transfer amount
     * @param description Remarks
     * @param securityPin 4-digit verification PIN
     * @return Operation result map
     */
    Map<String, Object> transferFunds(String customerId, String fromAccNumber, String toAccNumber, 
                                     double amount, String description, String securityPin);

    /**
     * Filters transactions based on criteria using Java 8 Streams.
     * @param customerId Optional customer ID filter
     * @param filterType Filter by type (DEPOSIT, WITHDRAWAL, TRANSFER)
     * @param searchQuery Search memo/account query
     * @param minAmount Minimum amount boundary
     * @param maxAmount Maximum amount boundary
     * @return List of matched transaction maps
     */
    List<Map<String, Object>> filterTransactions(String customerId, String filterType, 
                                                String searchQuery, Double minAmount, Double maxAmount);
}
