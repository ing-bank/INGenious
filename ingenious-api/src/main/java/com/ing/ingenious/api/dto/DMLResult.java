package com.ing.ingenious.api.dto;

/**
 * Represents the result of a DML (Data Manipulation Language) operation.
 * <p>
 * This class encapsulates both the success status and the executed query,
 * providing plugins with detailed feedback about database write operations.
 * </p>
 * 
 * @since 3.0
 */
public class DMLResult {
    /** Whether the DML operation succeeded */
    public final boolean success;
    
    /** The actual SQL query that was executed */
    public final String query;
    
    /**
     * Constructs a new DML result.
     * 
     * @param success true if the operation succeeded, false otherwise
     * @param query the SQL query that was executed
     */
    public DMLResult(boolean success, String query) {
        this.success = success;
        this.query = query;
    }
    
    /**
     * Returns whether the operation was successful.
     * 
     * @return true if successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * Returns the executed SQL query.
     * 
     * @return the SQL query string
     */
    public String getQuery() {
        return query;
    }
}
