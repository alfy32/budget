#!/bin/bash

# PostgreSQL database credentials
DB_NAME="budget_prod"
DB_USER="postgres"
DB_PASS="postgres"

# Backup directory and filename
BACKUP_DIR="/home/alan/budget-backups"
DATE=$(date +"%Y-%m-%d_%H-%M-%S")
BACKUP_FILE="$BACKUP_DIR/$DB_NAME-$DATE.sql"

# Execute pg_dump to backup the database
PGPASSWORD="$DB_PASS" pg_dump -U "$DB_USER" -h localhost "$DB_NAME" > "$BACKUP_FILE"

# Optional: Remove old backups (e.g., older than 7 days)
find "$BACKUP_DIR" -type f -name "*.sql" -mtime +30 -delete
