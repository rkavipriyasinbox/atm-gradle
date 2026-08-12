package com.assignment.atm.exception;

public class InsufficientFundsException extends AtmException {

    public InsufficientFundsException(int balance) {
        super("Insufficient funds. Your balance is $" + balance + ".");
    }
}

