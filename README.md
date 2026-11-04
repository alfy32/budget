# Budget

This is my budget application started in early 2024. I plan to use it in place of Mint.com since it no longer exists.

Version 2 is plain Spring Boot: Thymeleaf pages with [htmx](https://htmx.org) and [Alpine.js](https://alpinejs.dev)
for the bits that update without a reload, and a single SQLite file for the database. There is no npm or front-end build.
The original Angular + Postgres version is on the `v1` branch.

## Run it locally

```bash
./gradlew bootRun
```

Open http://localhost:8081. The database is `data/budget.db` (created on first start).

`bootRun` reads templates and CSS straight from `src/main/resources`:

- Templates, CSS and JavaScript: save, then refresh the browser. No restart.
- Java changes: devtools restarts the app in a second or two.

In IntelliJ, start it the same way: run the Gradle task **bootRun** (Gradle tool window → Tasks → application),
in Debug if you want Java method changes hot-swapped. Running `BudgetApplication` directly also works, but then
template edits only show up after a build (Ctrl+F9).

## Where things are

| What | Where |
|---|---|
| Pages | `src/main/resources/templates/*.html` (shared header in `layout.html`) |
| Styles | `src/main/resources/static/css/style.css` |
| URLs → pages | `src/main/java/com/alfy/budget/controller/` |
| SQL | `src/main/java/com/alfy/budget/service/` |
| Database tables | `src/main/resources/db/migration/` (add `V2__something.sql` to change them) |

## Look at or edit the data

The database is one file. In IntelliJ: **Database → + → Data Source → SQLite** and pick the file. Or:

```bash
sqlite3 data/budget.db
```

It's fine to edit it while the app is running.

## Copy data from the old Postgres version

Start the app once so it creates an empty `data/budget.db`, then:

```bash
./import_from_postgres.sh budget_prod data/budget.db
```

## Deploy

```bash
./update.sh
```

This builds a Docker image and runs it on port 8080 with the database at `/home/alan/budget-data/budget.db`.
`backup.sh` copies that file into `/home/alan/budget-backups` and keeps 30 days.
