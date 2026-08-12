package com.assignment.atm.exception;

public class SelfTransferException extends AtmException {

    public SelfTransferException() {
        super("Cannot transfer to yourself.");
    }
}

