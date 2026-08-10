package com.ing.exceptions;

/**
 * Exception thrown when duplicate methods are detected during method information loading.
 * <p>
 * This exception is typically thrown by the MethodInfoManager when multiple methods
 * with the same signature are found, which would create ambiguity in the framework.
 * </p>
 *
 * @see com.ing.engine.support.methodInf.MethodInfoManager
 */
public class DuplicateMethodException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new DuplicateMethodException with the specified detail message.
     *
     * @param message the detail message explaining the duplicate method conflict
     */
    public DuplicateMethodException(String message) {
        super(message);
    }
}
