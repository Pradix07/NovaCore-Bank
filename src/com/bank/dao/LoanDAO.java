package com.bank.dao;

import com.bank.exceptions.DatabaseException;
import com.bank.model.LoanApplication;
import java.util.List;

/**
 * ============================================================================
 * INTERFACE: LoanDAO
 * ============================================================================
 * Demonstrates:
 * - Data Access Object pattern for loan applications and status tracking
 */
public interface LoanDAO extends GenericDAO<LoanApplication, String> {

    /**
     * Retrieves all loan applications submitted by a specific customer.
     * @param customerId The customer ID
     * @return List of loan applications
     * @throws DatabaseException on SQL execution failure
     */
    List<LoanApplication> findByCustomerId(String customerId) throws DatabaseException;

    /**
     * Retrieves loans filtered by approval status (e.g., PENDING, APPROVED, REJECTED).
     * @param status The status string
     * @return List of matching loans
     * @throws DatabaseException on SQL execution failure
     */
    List<LoanApplication> findByStatus(String status) throws DatabaseException;

    /**
     * Updates loan approval status and review remarks.
     * @param loanId The loan ID
     * @param newStatus New status (APPROVED / REJECTED)
     * @param remarks Admin review notes
     * @return true if updated, false otherwise
     * @throws DatabaseException on SQL execution failure
     */
    boolean updateStatus(String loanId, String newStatus, String remarks) throws DatabaseException;
}
