package com.bank.exceptions;

/**
 * Thrown when a withdrawal violates minimum balance or maximum limit constraints.
 */
public class MaxWithdrawLimitException extends BankingException {
    private static final long serialVersionUID = 1L;

    public MaxWithdrawLimitException(String message) {
        super(message);
    }
}
