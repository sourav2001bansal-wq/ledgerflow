# LedgerFlow

A Java/Spring Boot **wallet and money-transfer simulator** built as a learning and portfolio project. All amounts are simulated INR. Milestone 1 starts on 2 October 2026.

## Start here

1. Install a Java 17+ JDK and Maven 3.6.3+ (or use IntelliJ's bundled Maven).
2. Extract this project and open the folder containing `pom.xml` in IntelliJ. Choose **Open as project**, select a Java 17 SDK, and allow Maven to import dependencies.
3. Run `LedgerFlowApplication.main()` from IntelliJ, or run `mvn spring-boot:run` in a terminal from this folder.
4. The server listens at `http://127.0.0.1:8080`. There is no home page yet; this milestone is an API backend.
5. In a separate PowerShell terminal run `./scripts/demo.ps1`. Alternative: `python scripts/demo.py`.

The demo creates Sourav's wallet with INR 1,000 and a friend's wallet with INR 200, transfers INR 500 and repeats the same request. The expected balances are INR 500 and INR 700. The repeated request returns the existing transfer without a second debit.

Read [Lesson 1](docs/LESSON_01.md) before modifying the code. API details and HTTP examples are in [API.md](docs/API.md).

## Implemented

- Create a wallet with simulated opening funds; read its balance.
- Transfer between two wallets atomically.
- Record a balanced, two-entry journal for transfers and opening funds.
- Return paginated wallet ledger history.
- Prevent duplicate transfers using a client-supplied UUID request ID.
- Reject request-ID reuse with different transfer details.
- Use database row locks to prevent concurrent overspending; acquire both locks in deterministic UUID order.
- Validate positive amounts, two decimal places, required fields and pagination bounds.
- Version the database schema with Flyway; validate it against the JPA mappings.
- Test failures, rollback, concurrent transfers and HTTP validation against H2.

## What "ledger" means here

A wallet ledger delta is positive for funds received and negative for funds sent. Each journal sums to zero. Simulated opening funds are offset by a negative `SIMULATED_FUNDING` entry; that account is not a user wallet. This is a deliberately small signed-entry ledger model, not a complete bank accounting system.

The wallet balance is the current materialized balance. Its wallet-specific ledger deltas should add up to that balance. Transactions keep them consistent. No endpoint updates or deletes ledger entries; database administrator immutability is not enforced in this milestone.

## Run verification

```sh
mvn verify
```

Tests use an isolated in-memory database, whereas the application uses a persistent local H2 file in `data/`. The API demo creates fresh wallets every run and does not erase previous data. Maven's XML reports are produced under `target/surefire-reports/`.

## Docker option

```sh
docker compose up --build
```

The API is exposed only on your machine at port 8080, and a named volume preserves the H2 database. Docker is an optional convenience; the first learning session can use IntelliJ. Docker execution is separate from the Maven tests and is not claimed verified unless recorded in `docs/VERIFICATION.md`.

## Optional PostgreSQL configuration

Set `DB_URL`, `DB_USER` and `DB_PASSWORD`, then start with the `postgres` Spring profile. Keep credentials out of Git. Example database URL: `jdbc:postgresql://localhost:5432/ledgerflow`. H2 tests do not prove PostgreSQL concurrency behavior; a real PostgreSQL integration suite belongs in the next milestone.

## Source layout

```text
src/main/java/dev/ledgerflow/
  WalletController.java       HTTP routes and input validation
  Api.java                    request and response DTOs
  WalletService.java          transaction boundary and transfer rules
  Wallet.java                 current wallet state
  Transfer.java               immutable completed transfer details
  LedgerEntry.java            signed journal entries
  *Repository.java           persistence queries and wallet locks
  ApiExceptionHandler.java   stable API error responses
src/main/resources/db/migration/
  V1__wallet_ledger.sql        versioned schema
src/test/java/dev/ledgerflow/
  WalletIntegrationTest.java  database, concurrent and HTTP tests
```

## Publish your own GitHub repository

This package is GitHub-ready, but it is **not yet hosted in a GitHub repository**. No account or repository URL is invented. The included workflow will run Maven tests after you publish it; its first cloud run must still be verified.

Create an empty repository named `ledgerflow` on your GitHub account, without a generated README. From the project folder:

```sh
git init
git add .
git commit -m "Build wallet transfer and balanced ledger milestone"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/ledgerflow.git
git push -u origin main
```

Replace `YOUR_USERNAME` with your real GitHub username. Use GitHub's normal authentication flow, not credentials pasted into chat. `target/`, local database files and `.env` are ignored. Keep commit dates authentic. The repository becomes your shareable portfolio link once the push succeeds.

## Next milestones

1. **Reconciliation:** compare wallet balances with ledger totals and flag inconsistencies. Your first coding exercise is a smaller version of this.
2. **Authentication and ownership:** Spring Security; users may access and debit only their own wallets. Current endpoints are intentionally local, unauthenticated demo APIs.
3. **PostgreSQL tests:** verify row locks, deadlocks and idempotency against the intended database, with migrations and Testcontainers.
4. **Reliable notifications:** transactional outbox and Kafka consumer; notifications must not alter money movement correctness.
5. **Operations:** metrics, structured logs, trace IDs and failure handling.
6. **Demo UI and deployment:** dashboard and a documented public demonstration, after access control exists.

There are no live bank connections, production users, performance claims, Kafka, Redis or JWT implementation in this milestone. Describe only implemented features on your resume.

## Technical references

- https://docs.spring.io/spring-boot/3.5/system-requirements.html
- https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html
- https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html

Spring Boot 3.5.16 is used to stay within the Boot 3 family while learning. The version was confirmed available from Maven Central during this build. Review maintained versions and dependencies when developing beyond this milestone.
