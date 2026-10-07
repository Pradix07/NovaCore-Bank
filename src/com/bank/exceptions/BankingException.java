package com.bank.exceptions;

public class BankingException extends RuntimeException {
    private final int statusCode;

    public BankingException(String message) {
        super(message);
        this.statusCode = 400;
    }

    public BankingException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
