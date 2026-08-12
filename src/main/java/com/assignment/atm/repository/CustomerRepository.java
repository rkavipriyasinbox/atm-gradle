package com.assignment.atm.repository;

import com.assignment.atm.model.Customer;

import java.util.Collection;
import java.util.Optional;

/** Repository contract for customer persistence. */
public interface CustomerRepository {

    Optional<Customer> findByName(String name);

    Customer save(Customer customer);

    Collection<Customer> findAll();
}

