-- Spending comes out of checking. A budget with a transfer account (e.g. "Yearly") is covered by moving
-- money from that account into checking. needs_transferred = 1 means "checking hasn't been paid back yet".
--
-- transferred_from records which account did pay it back, once it's marked transferred. A transaction
-- that has been paid back is never flagged again, even if its category changes later.
ALTER TABLE transactions ADD COLUMN transferred_from TEXT;

-- Transactions in a budget with a transfer account that are already marked done were paid from that account.
UPDATE transactions
SET transferred_from = (
    SELECT budgets.transfer_account
    FROM categories
    JOIN budgets ON budgets.id = categories.budget_id
    WHERE categories.id = transactions.category_id
)
WHERE needs_transferred = 0
  AND category_id IN (
      SELECT categories.id
      FROM categories
      JOIN budgets ON budgets.id = categories.budget_id
      WHERE budgets.transfer_account IS NOT NULL AND budgets.transfer_account <> ''
  );

-- Every new transaction used to start flagged, whatever its category. Only flags in a budget with a
-- transfer account ever meant anything, so clear the rest.
UPDATE transactions
SET needs_transferred = 0
WHERE needs_transferred = 1
  AND (category_id IS NULL OR category_id NOT IN (
      SELECT categories.id
      FROM categories
      JOIN budgets ON budgets.id = categories.budget_id
      WHERE budgets.transfer_account IS NOT NULL AND budgets.transfer_account <> ''
  ));
