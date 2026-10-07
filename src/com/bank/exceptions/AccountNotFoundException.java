package com.bank.exceptions;

public class AccountNotFoundException extends BankingException {
    public AccountNotFoundException(String message) {
        super(message, 404);
    }
}
