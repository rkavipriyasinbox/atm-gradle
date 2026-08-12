package com.assignment.atm.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents an ATM customer with a name, balance, and a map of debts.
 *
 * <p>Debts are stored as: creditorName → amountOwed, using a LinkedHashMap
 * so that debts are paid back in the order they were incurred (FIFO).
 */
public class Customer {

    private final String name;
    private int balance;
    /** creditorName -> amount this customer owes to that creditor */
    private final Map<String, Integer> debts;

    public Customer(String name) {
        this.name = name;
        this.balance = 0;
        this.debts = new LinkedHashMap<>();
    }

    public String getName() {
        return name;
    }

    public int getBalance() {
        return balance;
    }

    /** Returns an unmodifiable view of this customer's debts. */
    public Map<String, Integer> getDebts() {
        return Collections.unmodifiableMap(debts);
    }

    public void addToBalance(int amount) {
        if (amount < 0) throw new IllegalArgumentException("Amount must be non-negative, got: " + amount);
        this.balance += amount;
    }

    public void deductFromBalance(int amount) {
        if (amount < 0) throw new IllegalArgumentException("Amount must be non-negative, got: " + amount);
        if (amount > this.balance) {
            throw new IllegalStateException("Cannot deduct " + amount + " from balance " + this.balance);
        }
        this.balance -= amount;
    }

    /** Returns the amount this customer owes to the given creditor (0 if no debt). */
    public int getDebt(String creditorName) {
        return debts.getOrDefault(creditorName, 0);
    }

    /** Adds (or accumulates) a debt to the specified creditor. */
    public void addDebt(String creditorName, int amount) {
        if (amount <= 0) return;
        debts.merge(creditorName, amount, Integer::sum);
    }

    /**
     * Reduces the debt owed to the given creditor.
     * If the payment equals or exceeds the debt, the debt entry is removed.
     */
    public void reduceDebt(String creditorName, int amount) {
        if (amount <= 0) return;
        int current = getDebt(creditorName);
        if (amount >= current) {
            debts.remove(creditorName);
        } else {
            debts.put(creditorName, current - amount);
        }
    }

    public boolean hasDebt(String creditorName) {
        return debts.containsKey(creditorName);
    }

    public boolean hasAnyDebts() {
        return !debts.isEmpty();
    }
}

