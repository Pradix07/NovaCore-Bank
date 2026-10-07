package com.bank.exceptions;

/**
 * Compatibility alias for InvalidAmountException.
 */
public class InvalidAmount extends InvalidAmountException {
    private static final long serialVersionUID = 1L;

    public InvalidAmount(String message) {
        super(message);
    }
}
