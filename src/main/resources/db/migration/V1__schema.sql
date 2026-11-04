-- Conventions:
--   ids are UUID strings
--   dates are 'YYYY-MM-DD' text
--   money is whole cents (INTEGER), always positive; transaction_type says which way it went
--   booleans are 0 / 1

CREATE TABLE budgets (
    id               TEXT PRIMARY KEY,
    name             TEXT    NOT NULL,
    amount           INTEGER NOT NULL DEFAULT 0, -- per month
    monthly          INTEGER NOT NULL DEFAULT 0, -- show on the monthly page
    transfer_account TEXT
);

CREATE TABLE categories (
    id        TEXT PRIMARY KEY,
    name      TEXT NOT NULL,
    budget_id TEXT REFERENCES budgets (id) ON DELETE SET NULL
);

-- Exactly what the bank gave us. Never edited by the app after import.
CREATE TABLE bank_transactions (
    id               TEXT PRIMARY KEY,
    account          TEXT    NOT NULL,
    transaction_type TEXT    NOT NULL CHECK (transaction_type IN ('credit', 'debit')),
    transaction_date TEXT    NOT NULL,
    post_date        TEXT,
    reference_number TEXT,
    description      TEXT,
    comments         TEXT,
    check_number     TEXT,
    amount           INTEGER NOT NULL,
    balance          INTEGER,
    csv              TEXT -- the raw CSV line, used to detect re-uploads
);

CREATE INDEX bank_transactions_account_reference ON bank_transactions (account, reference_number);
CREATE INDEX bank_transactions_account_csv ON bank_transactions (account, csv);
CREATE INDEX bank_transactions_date ON bank_transactions (transaction_date);

-- What you see and edit. One per bank transaction, or several when it is split.
CREATE TABLE transactions (
    id                  TEXT PRIMARY KEY,
    bank_transaction_id TEXT    NOT NULL REFERENCES bank_transactions (id),
    split_index         INTEGER NOT NULL DEFAULT -1, -- -1 = not split, otherwise 1, 2, 3...
    account             TEXT    NOT NULL,
    transaction_type    TEXT    NOT NULL CHECK (transaction_type IN ('credit', 'debit')),
    transaction_date    TEXT    NOT NULL,
    description         TEXT,
    amount              INTEGER NOT NULL,
    category_id         TEXT REFERENCES categories (id) ON DELETE SET NULL,
    tags                TEXT, -- comma separated
    notes               TEXT,
    needs_transferred   INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX transactions_bank_transaction ON transactions (bank_transaction_id);
CREATE INDEX transactions_category ON transactions (category_id);
CREATE INDEX transactions_date ON transactions (transaction_date);

-- Applied to new transactions when a bank export is uploaded.
CREATE TABLE categorize_rules (
    id          TEXT PRIMARY KEY,
    description TEXT NOT NULL, -- bank description must match exactly
    category_id TEXT NOT NULL REFERENCES categories (id) ON DELETE CASCADE
);
