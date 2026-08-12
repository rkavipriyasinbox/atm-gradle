package com.assignment.atm.service;

/** Defines ATM operations available to the currently logged-in session. */
public interface AtmService {

    /**
     * Logs in as the named customer, creating the account if it does not yet exist.
     *
     * @param name the customer name (case-sensitive)
     * @return greeting message with balance and any outstanding debts
     */
    String login(String name);

    /**
     * Logs out the currently logged-in customer.
     *
     * @return farewell message
     */
    String logout();

    /**
     * Deposits the given amount. Outstanding debts are paid off first (FIFO),
     * and the remainder is added to the customer's balance.
     *
     * @param amount positive integer amount to deposit
     * @return summary of auto-payments and final balance
     */
    String deposit(int amount);

    /**
     * Withdraws the given amount from the customer's balance.
     * Withdrawal cannot exceed the current balance — going into debt via withdrawal is not permitted.
     *
     * @param amount positive integer amount to withdraw
     * @return final balance message
     */
    String withdraw(int amount);

    /**
     * Transfers the given amount to the target customer.
     *
     * <p>If the target already owes the current customer, that debt is reduced first before
     * any real funds move. If the current customer's balance is insufficient for the remaining
     * amount, the shortfall is recorded as a debt the current customer owes to the target.
     *
     * @param targetName name of the recipient (must have logged in at least once)
     * @param amount     positive integer amount to transfer
     * @return transfer summary and balance/debt status
     */
    String transfer(String targetName, int amount);

    /** Returns {@code true} if a customer is currently logged in. */
    boolean isLoggedIn();
}

