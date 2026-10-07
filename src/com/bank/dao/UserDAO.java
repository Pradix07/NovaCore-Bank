package com.bank.dao;

import com.bank.exceptions.DatabaseException;
import com.bank.model.User;
import java.util.Optional;

/**
 * ============================================================================
 * INTERFACE: UserDAO
 * ============================================================================
 * Demonstrates:
 * - Interface Inheritance (extends GenericDAO<User, String>)
 * - OOP Abstraction for User database operations
 */
public interface UserDAO extends GenericDAO<User, String> {

    /**
     * Finds a user record by username using JDBC.
     * @param username The login username
     * @return Optional containing the User if found
     * @throws DatabaseException on SQL execution failure
     */
    Optional<User> findByUsername(String username) throws DatabaseException;

    /**
     * Checks if a username is already taken in the database.
     * @param username The username to check
     * @return true if exists, false otherwise
     * @throws DatabaseException on SQL execution failure
     */
    boolean existsByUsername(String username) throws DatabaseException;

    /**
     * Updates account status / freeze state for a user.
     * @param userId The user identifier
     * @param isFrozen Frozen status flag
     * @return true if updated successfully
     * @throws DatabaseException on SQL execution failure
     */
    boolean setUserFrozenStatus(String userId, boolean isFrozen) throws DatabaseException;
}
