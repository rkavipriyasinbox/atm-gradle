package com.assignment.atm.service;

import com.assignment.atm.exception.*;
import com.assignment.atm.model.Customer;
import com.assignment.atm.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Core ATM business logic.
 *
 * <h2>Design decisions</h2>
 * <ul>
 *   <li><b>Session state</b>: a single {@code currentCustomer} field keeps track of who is
 *       logged in. This is appropriate for a single-user CLI; a multi-user REST API would
 *       hold session state per connection/token instead.</li>
 *   <li><b>Debt ordering (FIFO)</b>: debts are stored in a {@link LinkedHashMap} so that when
 *       a deposit auto-pays debts the oldest obligation is settled first.</li>
 *   <li><b>Transfer netting</b>: when A transfers to B and B already owes A, the outstanding
 *       debt is reduced before any real funds move. This prevents circular money flows and
 *       matches the example session in the spec.</li>
 *   <li><b>Withdrawal</b>: cannot exceed the current balance — the spec's debt mechanism is
 *       intentionally limited to the transfer command.</li>
 *   <li><b>Customer lookup</b>: transfer targets must already exist (have logged in at least
 *       once). Allowing transfers to unknown names would silently create unreachable accounts.</li>
 *   <li><b>Name case-sensitivity</b>: names are treated as-is (case-sensitive) to preserve
 *       user intent; "Alice" and "alice" are different customers.</li>
 * </ul>
 */
@Service
public class AtmServiceImpl implements AtmService {

    private final CustomerRepository repository;
    /** Holds the currently authenticated customer; {@code null} when no session is active. */
    private Customer currentCustomer;

    public AtmServiceImpl(CustomerRepository repository) {
        this.repository = repository;
    }

    // -------------------------------------------------------------------------
    // Session management
    // -------------------------------------------------------------------------

    @Override
    public String login(String name) {
        if (currentCustomer != null) {
            throw new AlreadyLoggedInException(currentCustomer.getName());
        }
        if (name == null || name.isBlank()) {
            throw new InvalidAmountException("Name cannot be empty.");
        }

        Customer customer = repository.findByName(name)
                .orElseGet(() -> repository.save(new Customer(name)));
        this.currentCustomer = customer;

        StringBuilder sb = new StringBuilder();
        sb.append("Hello, ").append(name).append("!\n");
        sb.append(formatBalance(customer));
        appendAllDebts(sb, customer);
        return sb.toString().trim();
    }

    @Override
    public String logout() {
        requireLogin();
        String name = currentCustomer.getName();
        currentCustomer = null;
        return "Goodbye, " + name + "!";
    }

    @Override
    public boolean isLoggedIn() {
        return currentCustomer != null;
    }

    // -------------------------------------------------------------------------
    // Transactions
    // -------------------------------------------------------------------------

    @Override
    public String deposit(int amount) {
        requireLogin();
        requirePositive(amount);

        StringBuilder sb = new StringBuilder();
        int remaining = amount;

        // Pay off debts in FIFO order (snapshot to avoid ConcurrentModificationException)
        Map<String, Integer> snapshot = new LinkedHashMap<>(currentCustomer.getDebts());
        for (Map.Entry<String, Integer> entry : snapshot.entrySet()) {
            if (remaining <= 0) break;

            String creditorName = entry.getKey();
            int debt = currentCustomer.getDebt(creditorName); // re-read in case it changed
            int payment = Math.min(remaining, debt);

            currentCustomer.reduceDebt(creditorName, payment);
            repository.findByName(creditorName).ifPresent(c -> c.addToBalance(payment));
            remaining -= payment;

            sb.append("Transferred $").append(payment).append(" to ").append(creditorName).append("\n");
        }

        // Any remainder stays with the depositor
        if (remaining > 0) {
            currentCustomer.addToBalance(remaining);
        }

        sb.append(formatBalance(currentCustomer));
        appendOwedTo(sb, currentCustomer);  // show remaining obligations after deposit
        return sb.toString().trim();
    }

    @Override
    public String withdraw(int amount) {
        requireLogin();
        requirePositive(amount);

        if (amount > currentCustomer.getBalance()) {
            throw new InsufficientFundsException(currentCustomer.getBalance());
        }

        currentCustomer.deductFromBalance(amount);
        return formatBalance(currentCustomer).trim();
    }

    @Override
    public String transfer(String targetName, int amount) {
        requireLogin();
        requirePositive(amount);

        if (currentCustomer.getName().equals(targetName)) {
            throw new SelfTransferException();
        }

        Customer target = repository.findByName(targetName)
                .orElseThrow(() -> new CustomerNotFoundException(targetName));

        StringBuilder sb = new StringBuilder();
        int remaining = amount;

        // Step 1 — net off any debt the target owes the current customer first.
        // This avoids a circular flow: instead of Alice paying Bob when Bob already owes Alice,
        // we simply reduce Bob's outstanding obligation.
        int targetDebtToCurrent = target.getDebt(currentCustomer.getName());
        if (targetDebtToCurrent > 0) {
            int reduction = Math.min(targetDebtToCurrent, remaining);
            target.reduceDebt(currentCustomer.getName(), reduction);
            remaining -= reduction;
            // No "Transferred" line here — no actual funds changed hands
        }

        // Step 2 — move real funds for whatever is left
        if (remaining > 0) {
            int actualTransfer = Math.min(currentCustomer.getBalance(), remaining);
            if (actualTransfer > 0) {
                currentCustomer.deductFromBalance(actualTransfer);
                target.addToBalance(actualTransfer);
                sb.append("Transferred $").append(actualTransfer).append(" to ").append(targetName).append("\n");
                remaining -= actualTransfer;
            }

            // Step 3 — record any shortfall as a debt
            if (remaining > 0) {
                currentCustomer.addDebt(targetName, remaining);
            }
        }

        sb.append(formatBalance(currentCustomer));

        // Show debt current customer owes to target (if any)
        int currentOwesTarget = currentCustomer.getDebt(targetName);
        if (currentOwesTarget > 0) {
            sb.append("Owed $").append(currentOwesTarget).append(" to ").append(targetName).append("\n");
        }

        // Show remaining debt target owes current (if any, after netting)
        int targetOwesCurrent = target.getDebt(currentCustomer.getName());
        if (targetOwesCurrent > 0) {
            sb.append("Owed $").append(targetOwesCurrent).append(" from ").append(targetName).append("\n");
        }

        return sb.toString().trim();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void requireLogin() {
        if (currentCustomer == null) {
            throw new NotLoggedInException();
        }
    }

    private void requirePositive(int amount) {
        if (amount <= 0) {
            throw new InvalidAmountException("Amount must be a positive whole number, got: " + amount + ".");
        }
    }

    private String formatBalance(Customer customer) {
        return "Your balance is $" + customer.getBalance() + "\n";
    }

    /** Appends both "owed to" and "owed from" lines — used after login. */
    private void appendAllDebts(StringBuilder sb, Customer customer) {
        appendOwedTo(sb, customer);
        appendOwedFrom(sb, customer);
    }

    /** Lines for what the current customer owes to others. */
    private void appendOwedTo(StringBuilder sb, Customer customer) {
        customer.getDebts().forEach((creditor, amount) ->
                sb.append("Owed $").append(amount).append(" to ").append(creditor).append("\n"));
    }

    /** Lines for what others owe the current customer. */
    private void appendOwedFrom(StringBuilder sb, Customer customer) {
        repository.findAll().stream()
                .filter(c -> !c.getName().equals(customer.getName()))
                .forEach(c -> {
                    int debt = c.getDebt(customer.getName());
                    if (debt > 0) {
                        sb.append("Owed $").append(debt).append(" from ").append(c.getName()).append("\n");
                    }
                });
    }
}

