# Verification — 2 October 2026

## Completed

- Compiled with OpenJDK 17.0.20 and Maven 3.9.9.
- Spring Boot 3.5.16 dependencies resolved from Maven Central.
- Ran the Spring Boot integration suite: **10 tests, 0 failures, 0 errors, 0 skipped**.
- Packaged the executable Spring Boot JAR successfully.
- Started the packaged JAR against an isolated H2 demo database and ran `scripts/demo.py` through HTTP.
- Demo result: transferred INR 500; source balance INR 500; destination balance INR 700; exact request replay returned the same response without another debit; source ledger showed +1,000 opening and -500 transfer.
- Confirmed that the packaged application also starts with the default local file-backed H2 configuration. The HTTP demo used a separate in-memory database, not the persistent database.

## Test cases

1. INR 500 transfer, wallet totals and zero-sum journal totals.
2. Insufficient funds leaves balances and history unchanged.
3. Injected ledger persistence failure rolls the transaction back.
4. Identical request-ID replay produces no additional debit.
5. Reused request ID with changed details is rejected.
6. Concurrent withdrawals cannot overdraw one source wallet.
7. Concurrent identical requests complete one money movement.
8. Opposite-direction transfers complete in the H2 fixture.
9. Invalid monetary amounts, missing fields and blank owner are rejected through HTTP.
10. Missing wallets, same-wallet transfers and invalid pagination return expected HTTP errors.

## Corrections made during verification

- Configured Mockito as a JVM startup agent for the Maven test runner because dynamic agent attachment was unavailable in this execution environment.
- Normalized persisted entity timestamps to microsecond precision. The initial response and database-loaded replay otherwise differed by nanoseconds due to database precision; the exact replay test now passes without weakening the assertion.

## Not verified yet

- PostgreSQL execution, locking behavior or Testcontainers integration.
- Docker build/runtime.
- Windows PowerShell execution of `scripts/demo.ps1` (the equivalent Python HTTP demo ran successfully).
- GitHub Actions cloud execution or repository publishing.
- Authentication, user authorization, production deployment, load or security testing.

The distributed ZIP includes source and documentation, not the built JAR or the local database. Build locally with Maven or IntelliJ.
