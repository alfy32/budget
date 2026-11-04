-- Budget plans: the budget set-up you agree on at a planning sit-down, from a start month until the next
-- plan starts. A plan without a start month is a proposal: nothing uses it until you commit it to a month.
--
--   plans              start month, take-home pay per month, the savings per month you want to stay above
--   plan_lines         one per category in the plan: which budget it belongs to and how much per year
--   plan_income_items  money set aside from income that isn't a spending category (e.g. a Roth IRA)
--
-- A budget's amount in a plan is the sum of its lines. Reports for a month use the plan in effect that month.
-- Savings per month = take-home - from income (donations + income items) - spending (monthly + yearly).
CREATE TABLE plans (
    id             TEXT PRIMARY KEY,
    start_month    TEXT UNIQUE, -- 'YYYY-MM-01', or null for a proposed plan
    take_home      INTEGER NOT NULL DEFAULT 0, -- cents per month
    savings_target INTEGER, -- cents per month, or null
    notes          TEXT
);

CREATE TABLE plan_lines (
    plan_id     TEXT    NOT NULL REFERENCES plans (id) ON DELETE CASCADE,
    category_id TEXT    NOT NULL REFERENCES categories (id) ON DELETE CASCADE,
    budget_id   TEXT    NOT NULL REFERENCES budgets (id) ON DELETE CASCADE,
    amount      INTEGER NOT NULL DEFAULT 0, -- cents per year
    PRIMARY KEY (plan_id, category_id)
);

CREATE INDEX plan_lines_budget ON plan_lines (budget_id);

CREATE TABLE plan_income_items (
    id      TEXT PRIMARY KEY,
    plan_id TEXT    NOT NULL REFERENCES plans (id) ON DELETE CASCADE,
    name    TEXT    NOT NULL,
    amount  INTEGER NOT NULL DEFAULT 0 -- cents per year
);

-- Which part of the plan a budget belongs to:
--   monthly    regular spending out of checking
--   yearly     irregular spending, saved for monthly in Yearly Savings
--   income     only paid when there's income (e.g. donations); not part of spending
--   other      paid from somewhere else (e.g. HSA); not counted against savings
ALTER TABLE budgets ADD COLUMN plan_group TEXT NOT NULL DEFAULT 'monthly'
    CHECK (plan_group IN ('monthly', 'yearly', 'income', 'other'));

UPDATE budgets SET plan_group = 'yearly' WHERE transfer_account = 'Yearly';
UPDATE budgets SET plan_group = 'income' WHERE name = 'Donations';
UPDATE budgets SET plan_group = 'other' WHERE transfer_account IS NOT NULL AND transfer_account NOT IN ('', 'Yearly');

-- Start with one plan that matches today's set-up, so nothing changes until you make a new plan.
-- A budget's monthly amount is split evenly over its categories.
INSERT INTO plans (id, start_month)
VALUES ('00000000-0000-0000-0000-000000000001', '2024-01-01');

INSERT INTO plan_lines (plan_id, category_id, budget_id, amount)
SELECT '00000000-0000-0000-0000-000000000001',
       categories.id,
       budgets.id,
       budgets.amount * 12 / (SELECT count(*) FROM categories c2 WHERE c2.budget_id = budgets.id)
FROM categories
JOIN budgets ON budgets.id = categories.budget_id;
