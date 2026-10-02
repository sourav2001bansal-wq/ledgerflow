CREATE TABLE wallets (
    id UUID PRIMARY KEY,
    owner VARCHAR(80) NOT NULL,
    balance NUMERIC(19,2) NOT NULL CHECK (balance >= 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE transfers (
    id UUID PRIMARY KEY,
    from_wallet_id UUID NOT NULL REFERENCES wallets(id),
    to_wallet_id UUID NOT NULL REFERENCES wallets(id),
    amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CHECK (from_wallet_id <> to_wallet_id)
);
CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY,
    journal_id UUID NOT NULL,
    wallet_id UUID REFERENCES wallets(id),
    account VARCHAR(64) NOT NULL,
    delta NUMERIC(19,2) NOT NULL CHECK (delta <> 0),
    kind VARCHAR(20) NOT NULL CHECK (kind IN ('OPENING','TRANSFER')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ledger_wallet_time ON ledger_entries(wallet_id, created_at, id);
CREATE INDEX ledger_journal ON ledger_entries(journal_id);
