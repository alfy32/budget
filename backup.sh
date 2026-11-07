#!/bin/bash
# Copies the SQLite database to a dated file. Safe to run while the app is running.
# To restore: stop the app, copy a backup over budget.db (and delete budget.db-wal / budget.db-shm), start the app.
set -euo pipefail

DB_FILE="/home/alan/budget/data/budget.db"
BACKUP_DIR="/home/alan/budget/backup"
DATE=$(date +"%Y-%m-%d_%H-%M-%S")

mkdir -p "$BACKUP_DIR"
sqlite3 "$DB_FILE" ".backup '$BACKUP_DIR/budget-$DATE.db'"

# Keep 30 days of backups
find "$BACKUP_DIR" -type f -name "budget-*.db" -mtime +30 -delete
