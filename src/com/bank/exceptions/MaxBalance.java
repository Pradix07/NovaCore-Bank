package com.bank.exceptions;

/**
 * Compatibility alias for MaxBalanceLimitException / InsufficientFundsException.
 */
public class MaxBalance extends MaxBalanceLimitException {
    private static final long serialVersionUID = 1L;

    public MaxBalance(String message) {
        super(message);
    }
}
