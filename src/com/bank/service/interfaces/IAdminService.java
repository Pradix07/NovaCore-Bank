package com.bank.service.interfaces;

import com.bank.model.User;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * INTERFACE: IAdminService
 * ============================================================================
 * Demonstrates:
 * - Interface Segregation for Administrative Operations & Bank Governance
 */
public interface IAdminService {

    /**
     * Retrieves aggregated system metrics and KPI statistics.
     * @return Dashboard statistics map
     */
    Map<String, Object> getAdminDashboardMetrics();

    /**
     * Retrieves all registered users in the bank with balance totals.
     * @return List of user entity maps
     */
    List<Map<String, Object>> getAllUsersWithDetails();

    /**
     * Creates a new User (Customer or Admin) with initial account setup.
     * @param adminId Performing admin ID
     * @param username New username
     * @param password Password
     * @param fullName Full name
     * @param email Email address
     * @param phone Phone
     * @param role CUSTOMER or ADMIN
     * @param initialAccountType SAVINGS or CHECKING
     * @param initialBalance Initial deposit
     * @param address Residential address
     * @param panOrTaxId PAN or Tax ID
     * @return Created User entity
     */
    User createUser(String adminId, String username, String password, String fullName, 
                    String email, String phone, String role, String initialAccountType, 
                    double initialBalance, String address, String panOrTaxId);

    /**
     * Freezes or unfreezes a customer account.
     * @param adminId Performing admin ID
     * @param targetUserId Target user ID
     * @param active New active state (true = active, false = suspended)
     */
    void updateUserStatus(String adminId, String targetUserId, boolean active);

    /**
     * Deletes a user and cascades associated accounts.
     * @param adminId Performing admin ID
     * @param targetUserId Target user ID
     */
    void deleteUser(String adminId, String targetUserId);

    /**
     * Updates banking parameters and system settings.
     * @param adminId Performing admin ID
     * @param newSettings Settings map
     */
    void updateSystemSettings(String adminId, Map<String, Object> newSettings);

    /**
     * Retrieves all system security audit logs.
     * @return List of audit log maps
     */
    List<Map<String, Object>> getAllAuditLogs();
}
