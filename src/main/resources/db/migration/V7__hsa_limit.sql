-- HSA: your before-tax contribution plus the employer's must stay under the yearly legal limit, with at least
-- one paycheck's contribution to spare (payroll once put an extra paycheck in the wrong year).
--   plans.hsa_limit       the legal limit for the plan's year, cents per year (it changes every year)
--   plan_pay_items.hsa    the line counts toward the HSA total (your contribution, the employer's)
ALTER TABLE plans ADD COLUMN hsa_limit INTEGER;
ALTER TABLE plan_pay_items ADD COLUMN hsa INTEGER NOT NULL DEFAULT 0;
