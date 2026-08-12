package com.assignment.atm.exception;

public class CustomerNotFoundException extends AtmException {

    public CustomerNotFoundException(String name) {
        super("Customer '" + name + "' not found. They must log in at least once before receiving transfers.");
    }
}

