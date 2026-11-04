-- An account is a running total: every credit in its categories adds, every debit subtracts.
-- Example: "Gift Money Alan". The balance can go below zero.
CREATE TABLE accounts (
    id   TEXT PRIMARY KEY,
    name TEXT NOT NULL
);

-- A category can count toward one account (separate from which budget it is in).
ALTER TABLE categories ADD COLUMN account_id TEXT REFERENCES accounts (id) ON DELETE SET NULL;
