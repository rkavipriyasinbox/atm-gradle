package com.assignment.atm.exception;

public class NotLoggedInException extends AtmException {

    public NotLoggedInException() {
        super("No customer is currently logged in. Use 'login [name]' to log in.");
    }
}

