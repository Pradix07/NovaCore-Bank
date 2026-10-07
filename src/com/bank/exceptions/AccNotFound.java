package com.bank.exceptions;

/**
 * Compatibility alias for AccountNotFoundException.
 */
public class AccNotFound extends AccountNotFoundException {
    private static final long serialVersionUID = 1L;

    public AccNotFound(String message) {
        super(message);
    }
}
