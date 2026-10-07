package com.bank.exceptions;

public class ValidationException extends BankingException {
    public ValidationException(String message) {
        super(message, 422);
    }
}
