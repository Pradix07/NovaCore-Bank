package com.bank.exceptions;

public class AuthenticationException extends BankingException {
    public AuthenticationException(String message) {
        super(message, 401);
    }
}
