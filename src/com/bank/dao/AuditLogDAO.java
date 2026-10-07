package com.bank.dao;

import com.bank.exceptions.DatabaseException;
import com.bank.model.AuditLog;
import java.util.List;

/**
 * ============================================================================
 * INTERFACE: AuditLogDAO
 * ============================================================================
 * Demonstrates:
 * - Data Access Object for security, compliance, and activity auditing.
 */
public interface AuditLogDAO extends GenericDAO<AuditLog, String> {

    /**
     * Retrieves the latest audit logs up to a specified limit.
     * @param limit Max entries to fetch
     * @return List of recent audit logs
     * @throws DatabaseException on SQL execution failure
     */
    List<AuditLog> findRecentLogs(int limit) throws DatabaseException;

    /**
     * Retrieves audit logs for a specific user.
     * @param userId The user ID
     * @return List of user audit logs
     * @throws DatabaseException on SQL execution failure
     */
    List<AuditLog> findByUserId(String userId) throws DatabaseException;
}
