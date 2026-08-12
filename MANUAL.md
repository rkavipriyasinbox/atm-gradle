# ATM — Operation Manual

## Starting the Application

```bash
./start.sh
```

The script builds the project (if needed) and launches the interactive CLI.
> **Requirements:** Java 21+, internet access on first run (Gradle downloads dependencies).

---

## Commands

| Command | Description |
|---|---|
| `login [name]` | Log in as a customer. Creates the account automatically if this is the first login. |
| `deposit [amount]` | Deposit a positive whole-dollar amount. Outstanding debts are auto-paid first (oldest first). |
| `withdraw [amount]` | Withdraw a positive whole-dollar amount. Cannot exceed your current balance. |
| `transfer [target] [amount]` | Transfer funds to another registered customer. |
| `logout` | End the current session. |
| `help` | Display the command reference. |
| `exit` / `quit` | Exit the application. |

> Amounts are **whole positive integers** (no decimals, no negative values).

---

## Example Session

```
login Alice
→ Hello, Alice!
→ Your balance is $0

deposit 100
→ Your balance is $100

logout
→ Goodbye, Alice!

login Bob
→ Hello, Bob!
→ Your balance is $0

deposit 80
→ Your balance is $80

transfer Alice 50
→ Transferred $50 to Alice
→ Your balance is $30

transfer Alice 100
→ Transferred $30 to Alice
→ Your balance is $0
→ Owed $70 to Alice

deposit 30
→ Transferred $30 to Alice
→ Your balance is $0
→ Owed $40 to Alice

logout
→ Goodbye, Bob!

login Alice
→ Hello, Alice!
→ Your balance is $210
→ Owed $40 from Bob

transfer Bob 30
→ Your balance is $210
→ Owed $10 from Bob

logout
→ Goodbye, Alice!

login Bob
→ Hello, Bob!
→ Your balance is $0
→ Owed $10 to Alice

deposit 100
→ Transferred $10 to Alice
→ Your balance is $90

logout
→ Goodbye, Bob!
```

---

## Design Decisions & Assumptions

### Debt from transfers (overdraft-by-transfer)
When a customer transfers more than their balance:
- The available balance is transferred immediately.
- The shortfall is recorded as a **debt** the sender owes the recipient.

Withdrawal deliberately **cannot** create debt. Going below zero requires an explicit transfer; this avoids accidental overdrafts from simple withdrawals.

### Automatic debt repayment on deposit
On every `deposit`, outstanding debts are settled **before** the balance grows. Debts are paid in **FIFO order** (oldest first), which is fair and predictable.

### Transfer netting
When customer **A** transfers to customer **B** and **B already owes A**, the outstanding debt is reduced first before any real funds move. This prevents circular fund flows and keeps balances accurate without unnecessary round-trips.

### Transfer to unknown customers
Transfers to a name that has never logged in are **rejected** with an error. Creating invisible accounts from transfer commands would lead to orphaned balances with no owner able to claim them.

### Case-sensitive names
Customer names are case-sensitive (`Alice ≠ alice`). This matches typical banking practice where names are stored verbatim.

### No decimal / fractional amounts
All amounts are whole integers. Fractional cents are not supported — this matches the spec examples and keeps the implementation straightforward.

### Single-session CLI
Only one customer can be logged in at a time. A second `login` command is rejected until the current session is ended with `logout`. This mirrors a physical ATM where one card is inserted at a time.

### State isolation between runs
All customer data lives in a `ConcurrentHashMap` in the JVM heap. Stopping and restarting the process (`start.sh`) produces a completely clean slate — no database, no files on disk.

---

## Running the Tests

```bash
./gradlew test
```

Test results are written to `build/reports/tests/test/index.html`.

