package com.bank.exceptions;

/**
 * Thrown when an account balance exceeds allowed maximum balance caps (e.g., student protective caps).
 */
public class MaxBalanceLimitException extends BankingException {
    private static final long serialVersionUID = 1L;

    public MaxBalanceLimitException(String message) {
        super(message);
    }
}
