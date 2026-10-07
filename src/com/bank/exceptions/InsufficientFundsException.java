package com.bank.exceptions;

public class InsufficientFundsException extends BankingException {
    public InsufficientFundsException(String message) {
        super(message, 400);
    }
}
