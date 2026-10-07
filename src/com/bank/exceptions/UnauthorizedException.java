package com.bank.exceptions;

public class UnauthorizedException extends BankingException {
    public UnauthorizedException(String message) {
        super(message, 403);
    }
}
