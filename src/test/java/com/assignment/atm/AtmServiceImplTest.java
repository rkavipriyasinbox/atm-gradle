package com.assignment.atm;

import com.assignment.atm.exception.*;
import com.assignment.atm.repository.InMemoryCustomerRepository;
import com.assignment.atm.service.AtmService;
import com.assignment.atm.service.AtmServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link AtmServiceImpl}.
 *
 * Uses a real {@link InMemoryCustomerRepository} so no mocking is needed —
 * the repository is trivially simple and in-memory, making it a clean collaborator.
 */
class AtmServiceImplTest {

    private AtmService atm;

    @BeforeEach
    void setUp() {
        atm = new AtmServiceImpl(new InMemoryCustomerRepository());
    }

    // =========================================================================
    // LOGIN
    // =========================================================================

    @Nested
    @DisplayName("login")
    class LoginTests {

        @Test
        @DisplayName("creates a new customer and greets them with $0 balance")
        void login_newCustomer_createsAccountWithZeroBalance() {
            String result = atm.login("Alice");
            assertThat(result).contains("Hello, Alice!")
                              .contains("Your balance is $0");
        }

        @Test
        @DisplayName("returns existing customer on second login")
        void login_existingCustomer_returnsExistingAccount() {
            atm.login("Alice");
            atm.deposit(50);
            atm.logout();

            String result = atm.login("Alice");
            assertThat(result).contains("Hello, Alice!")
                              .contains("Your balance is $50");
        }

        @Test
        @DisplayName("shows debts owed TO others on login")
        void login_withDebts_showsOwedTo() {
            atm.login("Alice");
            atm.logout();

            atm.login("Bob");
            atm.transfer("Alice", 100); // Bob has $0 → owes Alice $100
            atm.logout();

            String result = atm.login("Bob");
            assertThat(result).contains("Owed $100 to Alice");
        }

        @Test
        @DisplayName("shows debts owed FROM others on login")
        void login_othersOweCurrentCustomer_showsOwedFrom() {
            atm.login("Alice");
            atm.logout();

            atm.login("Bob");
            atm.transfer("Alice", 100); // Bob owes Alice $100
            atm.logout();

            String result = atm.login("Alice");
            assertThat(result).contains("Owed $100 from Bob");
        }

        @Test
        @DisplayName("throws when already logged in")
        void login_alreadyLoggedIn_throws() {
            atm.login("Alice");
            assertThatThrownBy(() -> atm.login("Bob"))
                    .isInstanceOf(AlreadyLoggedInException.class);
        }

        @Test
        @DisplayName("throws when name is blank")
        void login_blankName_throws() {
            assertThatThrownBy(() -> atm.login("  "))
                    .isInstanceOf(InvalidAmountException.class);
        }

        @Test
        @DisplayName("isLoggedIn returns true after login")
        void isLoggedIn_afterLogin_returnsTrue() {
            atm.login("Alice");
            assertThat(atm.isLoggedIn()).isTrue();
        }
    }

    // =========================================================================
    // LOGOUT
    // =========================================================================

    @Nested
    @DisplayName("logout")
    class LogoutTests {

        @Test
        @DisplayName("says goodbye and clears session")
        void logout_loggedIn_clearsSession() {
            atm.login("Alice");
            String result = atm.logout();
            assertThat(result).contains("Goodbye, Alice!");
            assertThat(atm.isLoggedIn()).isFalse();
        }

        @Test
        @DisplayName("throws when not logged in")
        void logout_notLoggedIn_throws() {
            assertThatThrownBy(() -> atm.logout())
                    .isInstanceOf(NotLoggedInException.class);
        }
    }

    // =========================================================================
    // DEPOSIT
    // =========================================================================

    @Nested
    @DisplayName("deposit")
    class DepositTests {

        @Test
        @DisplayName("increases balance by deposited amount")
        void deposit_basic_increasesBalance() {
            atm.login("Alice");
            String result = atm.deposit(100);
            assertThat(result).contains("Your balance is $100");
        }

        @Test
        @DisplayName("auto-pays oldest debt first (FIFO)")
        void deposit_withDebt_autoPaysDebt() {
            // Setup: Alice needs to owe Bob
            atm.login("Bob");
            atm.logout();

            atm.login("Alice");
            atm.transfer("Bob", 70);  // Alice owes Bob $70

            String result = atm.deposit(30);
            assertThat(result)
                    .contains("Transferred $30 to Bob")
                    .contains("Your balance is $0")
                    .contains("Owed $40 to Bob");
        }

        @Test
        @DisplayName("fully clears debt and keeps surplus in balance")
        void deposit_surplusAfterDebt_updatesBalance() {
            atm.login("Bob");
            atm.logout();

            atm.login("Alice");
            atm.transfer("Bob", 30);  // Alice owes Bob $30

            String result = atm.deposit(50);
            assertThat(result)
                    .contains("Transferred $30 to Bob")
                    .contains("Your balance is $20")
                    .doesNotContain("Owed");
        }

        @Test
        @DisplayName("creditor's balance increases when debt is auto-paid")
        void deposit_creditorReceivesFunds() {
            atm.login("Bob");
            atm.logout();

            atm.login("Alice");
            atm.transfer("Bob", 50);  // Alice owes Bob $50
            atm.deposit(50);          // pays Bob $50
            atm.logout();

            String bobLogin = atm.login("Bob");
            assertThat(bobLogin).contains("Your balance is $50");
        }

        @Test
        @DisplayName("throws when not logged in")
        void deposit_notLoggedIn_throws() {
            assertThatThrownBy(() -> atm.deposit(100))
                    .isInstanceOf(NotLoggedInException.class);
        }

        @Test
        @DisplayName("throws on non-positive amount")
        void deposit_zeroAmount_throws() {
            atm.login("Alice");
            assertThatThrownBy(() -> atm.deposit(0))
                    .isInstanceOf(InvalidAmountException.class);
        }

        @Test
        @DisplayName("pays multiple debts in insertion order")
        void deposit_multipleDebts_paysInOrder() {
            atm.login("Bob");
            atm.logout();
            atm.login("Charlie");
            atm.logout();

            atm.login("Alice");
            atm.transfer("Bob", 30);     // Alice owes Bob $30 first
            atm.transfer("Charlie", 20); // Alice owes Charlie $20 second

            String result = atm.deposit(35);
            // Should pay Bob $30 fully, then Charlie $5
            assertThat(result)
                    .contains("Transferred $30 to Bob")
                    .contains("Transferred $5 to Charlie")
                    .contains("Your balance is $0")
                    .contains("Owed $15 to Charlie");
        }
    }

    // =========================================================================
    // WITHDRAW
    // =========================================================================

    @Nested
    @DisplayName("withdraw")
    class WithdrawTests {

        @Test
        @DisplayName("reduces balance by withdrawn amount")
        void withdraw_basic_reducesBalance() {
            atm.login("Alice");
            atm.deposit(100);
            String result = atm.withdraw(40);
            assertThat(result).contains("Your balance is $60");
        }

        @Test
        @DisplayName("throws InsufficientFundsException when amount exceeds balance")
        void withdraw_exceedsBalance_throws() {
            atm.login("Alice");
            atm.deposit(50);
            assertThatThrownBy(() -> atm.withdraw(100))
                    .isInstanceOf(InsufficientFundsException.class)
                    .hasMessageContaining("$50");
        }

        @Test
        @DisplayName("throws when not logged in")
        void withdraw_notLoggedIn_throws() {
            assertThatThrownBy(() -> atm.withdraw(50))
                    .isInstanceOf(NotLoggedInException.class);
        }

        @Test
        @DisplayName("throws on non-positive amount")
        void withdraw_negativeAmount_throws() {
            atm.login("Alice");
            atm.deposit(100);
            assertThatThrownBy(() -> atm.withdraw(-10))
                    .isInstanceOf(InvalidAmountException.class);
        }

        @Test
        @DisplayName("exact balance withdrawal leaves $0")
        void withdraw_exactBalance_leavesZero() {
            atm.login("Alice");
            atm.deposit(100);
            String result = atm.withdraw(100);
            assertThat(result).contains("Your balance is $0");
        }
    }

    // =========================================================================
    // TRANSFER
    // =========================================================================

    @Nested
    @DisplayName("transfer")
    class TransferTests {

        @BeforeEach
        void setUpBothCustomers() {
            atm.login("Alice");
            atm.deposit(100);
            atm.logout();

            atm.login("Bob");
            atm.deposit(80);
        }

        @Test
        @DisplayName("basic transfer moves funds correctly")
        void transfer_basic_movesFunds() {
            String result = atm.transfer("Alice", 50);
            assertThat(result)
                    .contains("Transferred $50 to Alice")
                    .contains("Your balance is $30");
        }

        @Test
        @DisplayName("creates debt when balance is insufficient")
        void transfer_insufficientBalance_createsDebt() {
            String result = atm.transfer("Alice", 100); // Bob has $80
            assertThat(result)
                    .contains("Transferred $80 to Alice")
                    .contains("Your balance is $0")
                    .contains("Owed $20 to Alice");
        }

        @Test
        @DisplayName("full over-transfer creates full debt when balance is zero")
        void transfer_zeroBalance_fullDebt() {
            atm.withdraw(80); // empty balance
            String result = atm.transfer("Alice", 50);
            assertThat(result)
                    .contains("Your balance is $0")
                    .contains("Owed $50 to Alice")
                    .doesNotContain("Transferred");
        }

        @Test
        @DisplayName("reduces target's debt when target owes current customer")
        void transfer_targetOwesCurrent_reducesDebt() {
            // Make Bob owe Alice: Bob transfers more than he has
            atm.transfer("Alice", 100); // Bob has $80, owes Alice $20
            atm.logout();

            atm.login("Alice");
            // Alice transfers $15 to Bob; Bob owes Alice $20 → reduce to $5
            String result = atm.transfer("Bob", 15);
            assertThat(result)
                    .doesNotContain("Transferred $15")
                    .contains("Your balance is $180") // Alice: 100 + 80 (transferred from Bob) = 180
                    .contains("Owed $5 from Bob");
        }

        @Test
        @DisplayName("nets debt fully then transfers remainder from balance")
        void transfer_targetDebtLessThanAmount_netsDebtThenTransfers() {
            atm.transfer("Alice", 100); // Bob has $80, owes Alice $20; Alice gains $80 → Alice total: 100+80=180
            atm.logout();

            atm.login("Alice");
            // Alice has $180, Bob owes Alice $20
            // Alice transfers $30 to Bob: net $20 debt (clears Bob's debt), then transfer $10 real funds
            String result = atm.transfer("Bob", 30);
            assertThat(result)
                    .contains("Transferred $10 to Bob")
                    .contains("Your balance is $170");
        }

        @Test
        @DisplayName("throws CustomerNotFoundException for unknown target")
        void transfer_unknownTarget_throws() {
            assertThatThrownBy(() -> atm.transfer("Unknown", 10))
                    .isInstanceOf(CustomerNotFoundException.class);
        }

        @Test
        @DisplayName("throws SelfTransferException when transferring to self")
        void transfer_toSelf_throws() {
            assertThatThrownBy(() -> atm.transfer("Bob", 10))
                    .isInstanceOf(SelfTransferException.class);
        }

        @Test
        @DisplayName("throws when not logged in")
        void transfer_notLoggedIn_throws() {
            atm.logout();
            assertThatThrownBy(() -> atm.transfer("Alice", 10))
                    .isInstanceOf(NotLoggedInException.class);
        }

        @Test
        @DisplayName("throws on non-positive amount")
        void transfer_zeroAmount_throws() {
            assertThatThrownBy(() -> atm.transfer("Alice", 0))
                    .isInstanceOf(InvalidAmountException.class);
        }
    }

    // =========================================================================
    // FULL EXAMPLE SESSION (from the spec)
    // =========================================================================

    @Nested
    @DisplayName("full example session from spec")
    class FullSessionTest {

        @Test
        @DisplayName("reproduces the complete ATM.md example")
        void fullSession() {
            // $ login Alice
            String s = atm.login("Alice");
            assertThat(s).contains("Hello, Alice!").contains("Your balance is $0");

            // $ deposit 100
            s = atm.deposit(100);
            assertThat(s).contains("Your balance is $100");

            // $ logout
            s = atm.logout();
            assertThat(s).contains("Goodbye, Alice!");

            // $ login Bob
            s = atm.login("Bob");
            assertThat(s).contains("Hello, Bob!").contains("Your balance is $0");

            // $ deposit 80
            s = atm.deposit(80);
            assertThat(s).contains("Your balance is $80");

            // $ transfer Alice 50
            s = atm.transfer("Alice", 50);
            assertThat(s).contains("Transferred $50 to Alice").contains("Your balance is $30");

            // $ transfer Alice 100   (Bob has $30, so transfer $30, owe $70)
            s = atm.transfer("Alice", 100);
            assertThat(s)
                    .contains("Transferred $30 to Alice")
                    .contains("Your balance is $0")
                    .contains("Owed $70 to Alice");

            // $ deposit 30   (pays $30 toward Alice debt)
            s = atm.deposit(30);
            assertThat(s)
                    .contains("Transferred $30 to Alice")
                    .contains("Your balance is $0")
                    .contains("Owed $40 to Alice");

            // $ logout
            s = atm.logout();
            assertThat(s).contains("Goodbye, Bob!");

            // $ login Alice
            s = atm.login("Alice");
            assertThat(s)
                    .contains("Hello, Alice!")
                    .contains("Your balance is $210")
                    .contains("Owed $40 from Bob");

            // $ transfer Bob 30   (Bob owes Alice $40; reduce by $30 → $10 remaining)
            s = atm.transfer("Bob", 30);
            assertThat(s)
                    .contains("Your balance is $210")
                    .contains("Owed $10 from Bob");

            // $ logout
            s = atm.logout();
            assertThat(s).contains("Goodbye, Alice!");

            // $ login Bob
            s = atm.login("Bob");
            assertThat(s)
                    .contains("Hello, Bob!")
                    .contains("Your balance is $0")
                    .contains("Owed $10 to Alice");

            // $ deposit 100   (pays $10 to Alice, keeps $90)
            s = atm.deposit(100);
            assertThat(s)
                    .contains("Transferred $10 to Alice")
                    .contains("Your balance is $90");

            // $ logout
            s = atm.logout();
            assertThat(s).contains("Goodbye, Bob!");
        }
    }
}


