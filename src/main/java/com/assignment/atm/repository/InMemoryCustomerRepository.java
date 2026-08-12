package com.assignment.atm.repository;

import com.assignment.atm.model.Customer;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe, in-memory implementation of {@link CustomerRepository}.
 *
 * <p>State lives entirely in the JVM heap; every process restart produces a clean slate,
 * satisfying the "no state carried over between runs" requirement.
 */
@Repository
public class InMemoryCustomerRepository implements CustomerRepository {

    private final Map<String, Customer> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Customer> findByName(String name) {
        return Optional.ofNullable(store.get(name));
    }

    @Override
    public Customer save(Customer customer) {
        store.put(customer.getName(), customer);
        return customer;
    }

    @Override
    public Collection<Customer> findAll() {
        return Collections.unmodifiableCollection(store.values());
    }
}

