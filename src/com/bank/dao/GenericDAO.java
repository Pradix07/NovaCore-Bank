package com.bank.dao;

import com.bank.exceptions.DatabaseException;
import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * GENERIC INTERFACE: GenericDAO<T, ID>
 * ============================================================================
 * Demonstrates:
 * - Generics & Type Parameterization (T: Entity type, ID: Primary Key type)
 * - Data Access Object (DAO) Pattern
 * - Separation of Concerns between Business Logic and Database Persistence
 * - Robust Exception Handling with custom DatabaseException
 *
 * @param <T>  The entity type managed by this DAO
 * @param <ID> The primary key identifier type
 */
public interface GenericDAO<T, ID> {

    /**
     * Persists a new entity record into the database via JDBC.
     * @param entity The entity instance to save.
     * @return true if inserted successfully, false otherwise.
     * @throws DatabaseException on SQL / JDBC connection failures.
     */
    boolean save(T entity) throws DatabaseException;

    /**
     * Finds an entity by its primary key ID using JDBC PreparedStatement.
     * @param id The unique identifier.
     * @return Optional containing the entity if found, or empty Optional.
     * @throws DatabaseException on SQL execution errors.
     */
    Optional<T> findById(ID id) throws DatabaseException;

    /**
     * Retrieves all entity records from the database table.
     * @return List of all entity instances.
     * @throws DatabaseException on SQL execution errors.
     */
    List<T> findAll() throws DatabaseException;

    /**
     * Updates an existing entity record in the database.
     * @param entity The updated entity instance.
     * @return true if updated, false otherwise.
     * @throws DatabaseException on SQL execution errors.
     */
    boolean update(T entity) throws DatabaseException;

    /**
     * Deletes an entity record by its unique identifier.
     * @param id The primary key ID of the entity to delete.
     * @return true if deleted, false otherwise.
     * @throws DatabaseException on SQL execution errors.
     */
    boolean deleteById(ID id) throws DatabaseException;
}
