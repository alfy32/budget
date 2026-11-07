-- A plan's pay, worked down from gross salary the way the planning sheet does:
--
--   gross salary
--   - before tax  (dental, HSA, accident plan, traditional 401k)  = taxable salary
--   - taxes       (federal, social security, medicare, state)
--   - after tax   (roth 401k)                                    = paycheck
--   - transfers   (through checking to elsewhere: roth IRA, missionaries)
--   - donations   (tithing as a % of gross, fast offerings, ...)  = net
--   - spending    (monthly + yearly budgets)                     = savings
--
-- Lines in the 'info' section (employer HSA, 401k match) and 'insurance' section (for the emergency fund)
-- don't change the paycheck. Any line marked retirement counts toward the retirement total.
ALTER TABLE plans ADD COLUMN gross_salary INTEGER NOT NULL DEFAULT 0; -- cents per year

CREATE TABLE plan_pay_items (
    id         TEXT PRIMARY KEY,
    plan_id    TEXT    NOT NULL REFERENCES plans (id) ON DELETE CASCADE,
    section    TEXT    NOT NULL CHECK (section IN ('pretax', 'tax', 'posttax', 'transfer', 'donation', 'info', 'insurance')),
    name       TEXT    NOT NULL,
    kind       TEXT    NOT NULL DEFAULT 'amount' CHECK (kind IN ('amount', 'percent')),
    base       TEXT    NOT NULL DEFAULT 'gross' CHECK (base IN ('gross', 'taxable')), -- what a percent is of
    value      INTEGER NOT NULL DEFAULT 0, -- cents per year, or a percent in hundredths (6.2% = 620)
    retirement INTEGER NOT NULL DEFAULT 0,
    sort       INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX plan_pay_items_plan ON plan_pay_items (plan_id);

-- Money set aside from income (V5) becomes transfers.
INSERT INTO plan_pay_items (id, plan_id, section, name, kind, value, sort)
SELECT id, plan_id, 'transfer', name, 'amount', amount, rowid FROM plan_income_items;

DROP TABLE plan_income_items;
