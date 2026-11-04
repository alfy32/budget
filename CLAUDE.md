# Budget app

Personal budget app (Spring Boot + Thymeleaf + htmx + Alpine.js + SQLite). See README.md for running it.

## Design for our phones first

Everything is designed to fit and work best on the two phones we actually use:

| Phone | Screen (CSS pixels) | Notes |
|---|---|---|
| **iPhone SE** (2nd/3rd gen) | **375 × 667** | The tightest screen. If it fits here, it fits. Safari. |
| **Moto G Stylus 2022** | **412 × 915** | Chrome on Android. |

Rules:

- Check every page at **375 × 667** (and 412 × 915) before calling UI work done. No sideways scrolling, nothing cut off,
  and the important thing (amount, balance, Save button) visible without scrolling where possible.
- Nothing may break at 320px wide; it just doesn't have to be pretty there.
- Tap targets at least 44px tall. Inputs use 16px text (smaller makes the iPhone zoom in on tap).
- Keep the bottom tab bar and iPhone safe areas (`env(safe-area-inset-*)`) in mind: fixed or sticky things must not
  hide content behind them.
- Desktop should also be pleasant to use (at 900px+ the tab bar becomes a left sidebar), but phones win any trade-off.
- Light and dark mode both work: use the color variables in `static/css/style.css`, never hard-coded colors.

## Conventions

- Pages are Thymeleaf templates in `src/main/resources/templates`; shared pieces (head, top bar, tab bar, icons) are in
  `layout.html`. Reuse the existing building blocks in `style.css` (`card`, `row`, `section-title`, `btn`, `hero`, ...)
  instead of adding one-off styles.
- No npm or front-end build. htmx and Alpine come from webjars in `build.gradle`; our own small script is
  `static/js/app.js` (keeps your place in transaction lists, keeps history to list → transaction).
- Going back must return to where you were: side trips from a transaction (category, tags, split) use
  `data-replace` so the phone's back button goes to the list, not the picker.
- Money is stored as whole cents; `transaction_type` ('credit' / 'debit') says which way it went.
- Budget amounts live in **plans** (`plans`, `plan_lines`, `plan_pay_items`): each plan runs from its start month
  until the next one; a plan with no start month is a proposal. Reports use the plan in effect for each month.
  `categories.budget_id` and `budgets.amount` only mirror the current plan (`PlansService.syncCurrentPlan`).
  A plan's pay is worked down from gross salary like the planning sheet (see `V6__plan_pay.sql`); the worksheet
  does the same math in JavaScript (`plan.html`) as `PlansService.totals` does in Java, so keep them in step.
- The repo is **public**: never commit personal data (amounts, pay, the database). The real plans are loaded from
  `data/seed_plans.sql`, which is git-ignored like the rest of `data/`.
- Schema changes go in a new `src/main/resources/db/migration/V<n>__name.sql`; never edit an applied migration.
- `./gradlew test` renders every page against a throwaway database; keep it passing and add new pages to it.
