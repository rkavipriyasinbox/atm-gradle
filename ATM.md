# ATM

## Problem Statement

You are asked to develop an interface, either via Command Line (CLI) or a browser based UI, to simulate an interaction of an ATM with a retail bank.

The submission must include an executable `start.sh` at the root. Running it should launch the application without
additional setup. `start.sh` executes in an environment with the latest versions the langauge such as
Java (`java` in `PATH`, `JAVA_HOME` set), Node.js (`node` and `npm` in `PATH`), or Dart (SDK in `PATH`).
Internet access is enabled so dependencies can be downloaded (e.g., `./gradlew build`, `npm install`, `dart pub get`).

Each invocation must produce a clean start with no state carried over from previous runs — e.g., if users are created, the
 process stopped, and `start.sh` is run again, no users should be registered.

## Functional Requirements

* `login [name]` - Logs in as this customer and creates the customer if not exist
* `deposit [amount]` - Deposits this amount to the logged in customer
* `withdraw [amount]` - Withdraws this amount from the logged in customer
* `transfer [target] [amount]` - Transfers this amount from the logged in customer to the target customer
* `logout` - Logs out of the current customer

## Example Session

Using CLI as an example, your application output should contain at least the following depending on the scenario and commands. But feel free 
to add extra output as you see fit.

```bash
$ login Alice
Hello, Alice!
Your balance is $0

$ deposit 100
Your balance is $100

$ logout
Goodbye, Alice!

$ login Bob
Hello, Bob!
Your balance is $0

$ deposit 80
Your balance is $80

$ transfer Alice 50
Transferred $50 to Alice
your balance is $30

$ transfer Alice 100
Transferred $30 to Alice
Your balance is $0
Owed $70 to Alice

$ deposit 30
Transferred $30 to Alice
Your balance is $0
Owed $40 to Alice

$ logout
Goodbye, Bob!

$ login Alice
Hello, Alice!
Your balance is $210
Owed $40 from Bob

$ transfer Bob 30
Your balance is $210
Owed $10 from Bob

$ logout
Goodbye, Alice!

$ login Bob
Hello, Bob!
Your balance is $0
Owed $10 to Alice

$ deposit 100
Transferred $10 to Alice
Your balance is $90

$ logout
Goodbye, Bob!
```

## Rules
- Application must run, and you must include an instruction manual for operation
- If there are special cases or ambiguity, you are expected to take your own decision on how it should be handled and explain your decision
- If there are any assumptions and/or deviations from the problem statement, you are expected to detail your reasoning
- Please submit the assignment as a Git archive
- Do not include any binary files and/or executables
- Do not push into any public code repository
- You are allowed to use any library, but you should not use any library that outright solves the problem
- You should implement at a level where they would be proud to have other engineers look and review the result
- All parts of the submission will be assessed include but no limited to:
   - Design choices
   - Implementation technique (including tests)
   - Exception handling and special case handling
- You are free to use AI for the assignment