-- Plans hold only the budget: each category's budget and amount. The pay math (gross salary down to savings,
-- retirement, the HSA check) is done in the planning spreadsheet instead, so its tables and columns go.
DROP TABLE plan_pay_items;
ALTER TABLE plans DROP COLUMN gross_salary;
ALTER TABLE plans DROP COLUMN hsa_limit;
ALTER TABLE plans DROP COLUMN take_home;
ALTER TABLE plans DROP COLUMN savings_target;

-- Lines are entered per year or per month (per paycheck was only for pay lines).
UPDATE plan_lines SET unit = 'year' WHERE unit = 'paycheck';
