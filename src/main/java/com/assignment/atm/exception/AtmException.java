package com.assignment.atm.exception;

/** Base runtime exception for all ATM domain errors. */
public class AtmException extends RuntimeException {

    public AtmException(String message) {
        super(message);
    }
}

