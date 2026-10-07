package com.bank.exceptions;

/**
 * Thrown when an invalid transaction amount is specified (<= 0).
 */
public class InvalidAmountException extends BankingException {
    private static final long serialVersionUID = 1L;

    public InvalidAmountException(String message) {
        super(message);
    }
}
