package com.bank.exceptions;

/**
 * Compatibility alias for MaxWithdrawLimitException.
 */
public class MaxWithdraw extends MaxWithdrawLimitException {
    private static final long serialVersionUID = 1L;

    public MaxWithdraw(String message) {
        super(message);
    }
}
