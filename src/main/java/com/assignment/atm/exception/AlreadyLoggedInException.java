package com.assignment.atm.exception;

public class AlreadyLoggedInException extends AtmException {

    public AlreadyLoggedInException(String currentName) {
        super("Already logged in as '" + currentName + "'. Please 'logout' first.");
    }
}

