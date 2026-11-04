#!/bin/bash
# Copies everything from the old Postgres database (v1 of the app) into the new SQLite file.
#
#   ./import_from_postgres.sh [postgres_db] [sqlite_file]
#   ./import_from_postgres.sh budget_prod data/budget.db      <- the defaults
#
# Start the new app once first so it creates the empty SQLite file and tables, then stop it.
# Safe to re-run: it refuses to import into a database that already has transactions.

set -euo pipefail

SOURCE_DB="${1:-budget_prod}"
TARGET_FILE="${2:-data/budget.db}"
export PGPASSWORD="${PGPASSWORD:-postgres}"
PSQL=(psql -h localhost -U postgres -d "$SOURCE_DB" -At -v ON_ERROR_STOP=1)

if [ ! -f "$TARGET_FILE" ] || [ -z "$(sqlite3 "$TARGET_FILE" "SELECT name FROM sqlite_master WHERE name = 'transactions'")" ]; then
    echo "❌ $TARGET_FILE has no tables yet. Start the app once (./gradlew bootRun) so it creates them, then stop it."
    exit 1
fi

if [ "$(sqlite3 "$TARGET_FILE" "SELECT count(*) FROM transactions")" != "0" ]; then
    echo "❌ $TARGET_FILE already has transactions. Delete the file, start the app once, and try again."
    exit 1
fi

echo "➡️  Copying $SOURCE_DB into $TARGET_FILE..."

# Postgres writes out INSERT statements (quote_nullable handles quotes and NULLs) and sqlite3 runs them.
# Old column names are mapped to the new snake_case ones here.
{
    echo "BEGIN;"

    "${PSQL[@]}" -c "
        SELECT format('INSERT INTO budgets (id, name, amount, monthly, transfer_account) VALUES (%s, %s, %s, %s, %s);',
            quote_nullable(id::text), quote_nullable(name), amount,
            CASE WHEN monthly THEN 1 ELSE 0 END, quote_nullable(transfer_account))
        FROM budgets"

    # A category pointing at a deleted budget becomes 'no budget'.
    "${PSQL[@]}" -c "
        SELECT format('INSERT INTO categories (id, name, budget_id) VALUES (%s, %s, %s);',
            quote_nullable(c.id::text), quote_nullable(c.name), quote_nullable(b.id::text))
        FROM categories c LEFT JOIN budgets b ON b.id = c.budgetid"

    "${PSQL[@]}" -c "
        SELECT format('INSERT INTO bank_transactions (id, account, transaction_type, transaction_date, post_date, reference_number, description, comments, check_number, amount, balance, csv) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s);',
            quote_nullable(id::text), quote_nullable(account), quote_nullable(transactiontype),
            quote_nullable(transactiondate::text), quote_nullable(postdate::text), quote_nullable(referencenumber),
            quote_nullable(description), quote_nullable(comments), quote_nullable(checknumber),
            amount, coalesce(balance::text, 'NULL'), quote_nullable(csv))
        FROM bank_transactions"

    # A transaction pointing at a deleted category becomes 'needs categorized', same as the old app showed it.
    # Transfers: v1 flagged every transaction. Only ones in a budget with a transfer account keep the flag;
    # ones there that were already marked done record that account as transferred_from.
    "${PSQL[@]}" -c "
        SELECT format('INSERT INTO transactions (id, bank_transaction_id, split_index, account, transaction_type, transaction_date, description, amount, category_id, tags, notes, needs_transferred, transferred_from) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s);',
            quote_nullable(t.id::text), quote_nullable(t.banktransactionid::text), coalesce(t.splitindex, -1),
            quote_nullable(t.account), quote_nullable(t.transactiontype), quote_nullable(t.transactiondate::text),
            quote_nullable(t.description), t.amount, quote_nullable(c.id::text),
            quote_nullable(t.tags), quote_nullable(t.notes),
            CASE WHEN coalesce(t.needs_transferred, false) AND coalesce(b.transfer_account, '') <> '' THEN 1 ELSE 0 END,
            quote_nullable(CASE WHEN NOT coalesce(t.needs_transferred, false) AND coalesce(b.transfer_account, '') <> '' THEN b.transfer_account END))
        FROM transactions t
        LEFT JOIN categories c ON c.id = t.categoryid
        LEFT JOIN budgets b ON b.id = c.budgetid"

    # The rules that used to be hard-coded in AutoCategorizeService.java.
    "${PSQL[@]}" -c "
        SELECT DISTINCT ON (r.description) format('INSERT INTO categorize_rules (id, description, category_id) VALUES (%s, %s, %s);',
            quote_literal(gen_random_uuid()::text), quote_literal(r.description), quote_literal(c.id::text))
        FROM (VALUES
            ('VANGUARD BUY/INVESTMENT', 'Investments'),
            ('Ch JesusChrist/DONATION', 'Donations'),
            ('Check Payroll/PAY', 'Paycheck'),
            ('EddyHR Payroll/PAY', 'Paycheck'),
            ('CENTRA COM/BILLPAY', 'Internet')
        ) AS r(description, category)
        JOIN categories c ON c.name = r.category"

    echo "COMMIT;"
} | sqlite3 -bail "$TARGET_FILE"

# Your budget plans live in data/seed_plans.sql (not committed: the repo is public). Load them if present.
SEED_FILE="$(dirname "$0")/data/seed_plans.sql"
if [ -f "$SEED_FILE" ]; then
    echo "➡️  Loading budget plans from $SEED_FILE..."
    sqlite3 -bail "$TARGET_FILE" < "$SEED_FILE"
else
    echo "ℹ️  No $SEED_FILE, so there's one plan built from the v1 budget amounts. Set up plans in the app."
fi

echo "✅ Done. Rows in $TARGET_FILE:"
sqlite3 -column -header "$TARGET_FILE" "
    SELECT 'budgets' AS 'table', count(*) AS 'rows' FROM budgets
    UNION ALL SELECT 'categories', count(*) FROM categories
    UNION ALL SELECT 'bank_transactions', count(*) FROM bank_transactions
    UNION ALL SELECT 'transactions', count(*) FROM transactions
    UNION ALL SELECT 'categorize_rules', count(*) FROM categorize_rules"
