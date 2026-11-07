-- The unit each plan line is entered in: some amounts are decided per year (property tax), some per month
-- (internet), some per paycheck (HSA). Amounts are still stored per year; this is only how the box shows them.
ALTER TABLE plan_lines ADD COLUMN unit TEXT NOT NULL DEFAULT 'year' CHECK (unit IN ('year', 'month', 'paycheck'));
ALTER TABLE plan_pay_items ADD COLUMN unit TEXT NOT NULL DEFAULT 'year' CHECK (unit IN ('year', 'month', 'paycheck'));
