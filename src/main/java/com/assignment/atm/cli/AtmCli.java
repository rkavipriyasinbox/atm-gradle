package com.assignment.atm.cli;

import com.assignment.atm.exception.AtmException;
import com.assignment.atm.exception.InvalidAmountException;
import com.assignment.atm.service.AtmService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Scanner;

/**
 * Interactive CLI loop for the ATM application.
 *
 * <p>Reads commands from {@link System#in} line by line and delegates to {@link AtmService}.
 * The loop exits when {@code exit} / {@code quit} is typed or the input stream closes (e.g. EOF).
 */
@Component
public class AtmCli implements CommandLineRunner {

    private static final String HELP_TEXT = """
            ┌──────────────────────────────────────────────────┐
            │                   ATM Commands                   │
            ├──────────────────────────────────────────────────┤
            │  login [name]               Log in / register    │
            │  deposit [amount]           Deposit funds        │
            │  withdraw [amount]          Withdraw funds       │
            │  transfer [target] [amount] Transfer to another  │
            │  logout                     End session          │
            │  help                       Show this help       │
            │  exit | quit                Exit the ATM         │
            └──────────────────────────────────────────────────┘
            Amounts must be positive whole numbers.""";

    private final AtmService atmService;

    public AtmCli(AtmService atmService) {
        this.atmService = atmService;
    }

    @Override
    public void run(String... args) {
        System.out.println("Welcome to the ATM. Type 'help' for available commands.");
        System.out.println();

        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }
                try {
                    processLine(line);
                } catch (Exception e) {
                    // Defensive guard: keep the ATM session alive even on unexpected runtime errors.
                    System.out.println("Unexpected error: " + e.getMessage());
                }
                System.out.println();
            }
        }
        // EOF reached — clean exit
        System.out.println("Session ended.");
    }

    private void processLine(String line) {
        String[] parts = line.split("\\s+");
        String command = parts[0].toLowerCase();

        try {
            String result = switch (command) {
                case "login" -> {
                    requireArgCount(parts, 2, "login [name]");
                    yield atmService.login(parts[1]);
                }
                case "deposit" -> {
                    requireArgCount(parts, 2, "deposit [amount]");
                    yield atmService.deposit(parseAmount(parts[1]));
                }
                case "withdraw" -> {
                    requireArgCount(parts, 2, "withdraw [amount]");
                    yield atmService.withdraw(parseAmount(parts[1]));
                }
                case "transfer" -> {
                    requireArgCount(parts, 3, "transfer [target] [amount]");
                    yield atmService.transfer(parts[1], parseAmount(parts[2]));
                }
                case "logout" -> atmService.logout();
                case "help" -> HELP_TEXT;
                case "exit", "quit" -> {
                    System.out.println("Goodbye!");
                    System.exit(0);
                    yield "";
                }
                default -> "Unknown command: '" + command + "'. Type 'help' for available commands.";
            };

            if (result != null && !result.isEmpty()) {
                System.out.println(result);
            }
        } catch (NumberFormatException e) {
            System.out.println("Error: '" + e.getMessage() + "' is not a valid amount. Please enter a whole number.");
        } catch (AtmException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (Exception e) {
            // Fallback for non-domain failures so the process does not terminate.
            System.out.println("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Validates that the command has at least {@code required} tokens.
     *
     * @throws InvalidAmountException with a usage hint if the check fails
     */
    private void requireArgCount(String[] parts, int required, String usage) {
        if (parts.length < required) {
            throw new InvalidAmountException("Usage: " + usage);
        }
    }

    /**
     * Parses an amount string to an {@code int}.
     *
     * @throws NumberFormatException carrying the original string so the error message is informative
     */
    private int parseAmount(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new NumberFormatException(s);
        }
    }
}

