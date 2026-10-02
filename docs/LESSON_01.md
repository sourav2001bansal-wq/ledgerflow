# Lesson 1 — a transfer that either completes or changes nothing

Your first goal is to run this sequence and explain it in your own words:

| Wallet | Before | Change | After |
| --- | ---: | ---: | ---: |
| Sourav | INR 1,000 | -500 | INR 500 |
| Friend | INR 200 | +500 | INR 700 |

The total remains INR 1,200. The transfer journal contains `-500` and `+500`, which sum to zero.

## Follow the request

1. `POST /api/transfers` reaches `WalletController.transfer()`.
2. `@Valid` checks the request DTO: IDs must exist in the JSON, money must be positive, and no more than two fractional digits are allowed.
3. `WalletService.transfer()` runs inside a transaction created by Spring's `@Transactional` proxy.
4. The service checks whether this `requestId` already completed. If so, it returns that transfer when all details match, or rejects conflicting reuse.
5. It sorts both wallet IDs and locks them in that order. Another transfer touching the same wallet must wait.
6. It checks the request ID again after acquiring locks, because another request may have finished while this one waited.
7. It checks sufficient funds, subtracts from the source, adds to the destination, and saves the transfer and two ledger rows.
8. The database commits all writes together. If the transaction throws an unchecked exception before completion, the database rolls the writes back.

The response reaches the client only after the transactional service proxy finishes its commit. A failure to deliver the HTTP response can still happen AFTER commit. That is why the client must retry with the **same request ID**, rather than making a new one.

## Why BigDecimal?

`double` represents many decimal amounts approximately. `BigDecimal` models these decimal values explicitly. Incoming JSON is mapped directly to `BigDecimal`, and the API rejects amounts with more than two decimal places rather than silently rounding them.

Create amounts from strings in Java examples:

```java
new BigDecimal("500.00")
```

Use `compareTo()` for numeric comparisons. `new BigDecimal("500.0").equals(new BigDecimal("500.00"))` is false because `equals()` also compares scale; `compareTo()` returns zero.

## Why locks?

Suppose INR 1,000 is available and two requests each try to send INR 800. Without coordination, both might read INR 1,000 and both might pass the funds check. A pessimistic database write lock makes the second request wait; it then sees the remaining INR 200 and fails.

Locking A then B in one transaction and B then A in another can create a deadlock. Sorting IDs before locking ensures that both transfers request those two locks in the same order. Lock timeouts can still happen, so the API supports retryable conflict responses and idempotent retries.

## What the tests demonstrate

- A successful INR 500 transfer and balanced journals.
- Insufficient funds leaves both wallets and transfer history unchanged.
- An injected ledger write failure AFTER balance changes rolls back everything.
- A replay does not debit twice; changed details with the same ID are rejected.
- Two simultaneous withdrawals cannot overdraw the source.
- Two simultaneous identical requests debit once.
- Reverse-direction transfers finish without deadlocking in the H2 fixture.
- Invalid HTTP input is rejected before money changes.

These are integration tests with a real H2 database, not just Mockito return values. PostgreSQL behavior will need its own tests later.

## Your first exercise — own a real feature

Implement `GET /api/wallets/{id}/reconciliation` to return:

```json
{
  "walletId": "the requested UUID",
  "storedBalance": 500.00,
  "ledgerBalance": 500.00,
  "matches": true
}
```

Use all ledger entries for the wallet, including opening funds. Sum `delta`, compare with `BigDecimal.compareTo`, and handle wallets with no entries as zero. Do not derive the answer from just the first page of ledger results. Consider how you will make the two reads represent a consistent view while a transfer is happening.

Do this yourself after you understand the existing flow; send the code for review. It becomes a feature you can explain and defend in an interview.

## Questions to answer before moving on

1. Why does the service check idempotency twice?
2. What happens if the ledger save fails after `from.debit()`?
3. Why must retries keep the same request ID?
4. Why is `@Transactional` on the public service method, and what would self-invocation change?
5. Which guarantees have we tested on H2, and which still need PostgreSQL tests?
