package com.assignment.atm;

import com.assignment.atm.cli.AtmCli;
import com.assignment.atm.repository.InMemoryCustomerRepository;
import com.assignment.atm.service.AtmService;
import com.assignment.atm.service.AtmServiceImpl;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AtmCliTest {

    @Test
    void cliContinuesAfterInsufficientFundsError() {
        AtmService atmService = new AtmServiceImpl(new InMemoryCustomerRepository());
        AtmCli cli = new AtmCli(atmService);

        String input = String.join(System.lineSeparator(),
                "login Alice",
                "withdraw 100",
                "deposit 25") + System.lineSeparator();

        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;

        ByteArrayInputStream testIn = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream testOut = new ByteArrayOutputStream();

        try {
            System.setIn(testIn);
            System.setOut(new PrintStream(testOut, true, StandardCharsets.UTF_8));

            cli.run();
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
        }

        String output = testOut.toString(StandardCharsets.UTF_8);
        assertThat(output)
                .contains("Hello, Alice!")
                .contains("Error: Insufficient funds. Your balance is $0.")
                .contains("Your balance is $25")
                .contains("Session ended.");
    }
}

