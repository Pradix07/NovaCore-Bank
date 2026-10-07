package com.bank.model;

import com.bank.exceptions.InsufficientFundsException;

/**
 * ============================================================================
 * INTERFACE: AccountOperations
 * ============================================================================
 * Demonstrates:
 * - OOP Principle: Abstraction & Interface Design
 * - Decouples account transaction contracts from specific account implementations.
 * - Enforces polymorphic behavior for deposit, withdraw, and interest calculation.
 */
public interface AccountOperations {

    /**
     * Deposits money into the account with thread-safe concurrency lock.
     * @param amount The monetary amount to deposit (must be > 0).
     */
    void deposit(double amount);

    /**
     * Withdraws money from the account subject to type-specific balance rules.
     * @param amount The monetary amount to withdraw.
     * @throws InsufficientFundsException If withdrawal exceeds available funds/overdraft.
     */
    void withdraw(double amount) throws InsufficientFundsException;

    /**
     * Calculates the annual interest earned based on the specific account type.
     * @return Calculated annual interest in the account's currency.
     */
    double calculateAnnualInterest();

    /**
     * Returns the polymorphic classification of the account (e.g., SAVINGS, CHECKING).
     * @return String representing account type.
     */
    String getAccountType();
}
