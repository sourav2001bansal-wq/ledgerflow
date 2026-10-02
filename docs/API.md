# API contract — milestone 1

Base URL: `http://127.0.0.1:8080/api`. Currency is fixed to INR. Send `Content-Type: application/json`. Amounts must have at most two fractional digits.

## Create wallets

`POST /wallets` → `201 Created`

```json
{"owner":"Sourav","initialBalance":1000.00}
```

Owner is required and at most 80 characters. Initial balance must be between 0 and 1,000,000,000 INR. This is simulated funding, not a bank deposit. Save the returned `id`.

```json
{"id":"generated UUID","owner":"Sourav","currency":"INR","balance":1000.00,"createdAt":"UTC timestamp"}
```

## Read wallet

`GET /wallets/{id}` → `200 OK`, or `404` for a missing wallet.

## Transfer

`POST /transfers` → `200 OK` for both successful creation and an exact replay.

```json
{
  "requestId":"8d2dd1c4-b596-4fe0-9177-1c66806f785e",
  "fromWalletId":"replace with source wallet UUID",
  "toWalletId":"replace with destination wallet UUID",
  "amount":500.00
}
```

Amount is 0.01 through 1,000,000,000 INR. Create a new request UUID for each NEW intended transfer. Reuse that UUID when retrying the SAME intended transfer, especially after an HTTP timeout. IDs for source and destination must differ and reference existing wallets.

Response fields: `id` (same as requestId), `fromWalletId`, `toWalletId`, `currency`, `amount`, `status` (`COMPLETED`) and `createdAt`.

## Wallet ledger

`GET /wallets/{id}/ledger?page=0&size=20` → `200 OK`.

Response: `entries`, `page`, `size`, `totalElements`. Maximum page size is 100; page index starts at zero. Each entry contains its `id`, `journalId`, `walletId`, `account`, signed `delta`, `kind` and `createdAt`. Ordered by creation time and ID.

## Errors

| HTTP | Code | Meaning |
| --- | --- | --- |
| 400 | INVALID_REQUEST | JSON, fields, amount, UUID or pagination invalid |
| 400 | SAME_WALLET | Source and destination are the same |
| 404 | WALLET_NOT_FOUND | A referenced wallet is missing |
| 422 | INSUFFICIENT_FUNDS | Source balance cannot cover the amount |
| 409 | IDEMPOTENCY_CONFLICT | Same request ID reused for different details |
| 409 | WRITE_CONFLICT | Database constraint conflict; retry same ID to resolve result |
| 409 | RETRYABLE_LOCK_CONFLICT | Lock contention; retry the same ID |

Error body: `code`, `message`, `timestamp`. Unknown JSON fields are rejected to catch client mistakes. Unexpected server errors are not translated into successful transfers.

## Quick HTTP example

```sh
curl -X POST http://127.0.0.1:8080/api/wallets \
  -H 'Content-Type: application/json' \
  -d '{"owner":"Sourav","initialBalance":1000.00}'
```

For Windows PowerShell use the supplied `scripts/demo.ps1` rather than copying shell quoting directly.
