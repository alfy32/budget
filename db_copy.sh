#!/bin/bash

# --- Configuration ---
ADMIN_USER="postgres"        # User with CREATEDB/DROPDB privileges
ADMIN_DB_PASS="postgres"     # The password for the ADMIN_USER (used for creation/dropping)

SOURCE_DB_NAME="budget_prod"
TARGET_DB_NAME="budget_dev"

# Backup directory and filename
BACKUP_DIR="/home/alan/budget-backups"
DATE=$(date +"%Y-%m-%d_%H-%M-%S")
BACKUP_FILE="$BACKUP_DIR/$SOURCE_DB_NAME-$DATE.sql"

# --- Pre-Check ---
mkdir -p "$BACKUP_DIR"

# --- 1. Execute pg_dump to backup the source database ---
echo "✅ Starting backup of database '$SOURCE_DB_NAME' to '$BACKUP_FILE'..."
PGPASSWORD="$ADMIN_USER" pg_dump -U "$ADMIN_USER" -h localhost "$SOURCE_DB_NAME" -O -x > "$BACKUP_FILE"

if [ $? -ne 0 ]; then
    echo "❌ ERROR: pg_dump failed. Check password for '$ADMIN_USER' or permissions."
    exit 1
fi
echo "Backup complete."

# --- 2. Administrative Operations (Drop and Create) ---

# Use ADMIN_DB_PASS for the ADMIN_USER (needed for permissions check)
echo "🔍 Checking for existing target database '$TARGET_DB_NAME'..."
DB_EXISTS=$(PGPASSWORD="$ADMIN_DB_PASS" psql -U "$ADMIN_USER" -h localhost -lqt | cut -d \| -f 1 | grep -w "$TARGET_DB_NAME")

if [ -n "$DB_EXISTS" ]; then
    echo "🚨 Target database '$TARGET_DB_NAME' found. Dropping it..."
    # Drop the existing database using the ADMIN_USER
    PGPASSWORD="$ADMIN_DB_PASS" dropdb -U "$ADMIN_USER" -h localhost "$TARGET_DB_NAME" 2> /dev/null
    if [ $? -ne 0 ]; then
        echo "❌ ERROR: Could not drop existing database '$TARGET_DB_NAME'. Check ADMIN_USER permissions."
        exit 1
    fi
fi

# Create the new target database and set the ADMIN_USER as owner
echo "⚙️ Creating new database '$TARGET_DB_NAME' with owner '$ADMIN_USER'..."
PGPASSWORD="$ADMIN_DB_PASS" createdb -U "$ADMIN_USER" -h localhost -O "$ADMIN_USER" "$TARGET_DB_NAME"

if [ $? -ne 0 ]; then
    echo "❌ ERROR: createdb failed. Check ADMIN_USER permissions or if ADMIN_USER exists."
    exit 1
fi
echo "Database '$TARGET_DB_NAME' created."

# --- 3. Restore the backup file into the new database ---
echo "➡️ Restoring backup into '$TARGET_DB_NAME' using user '$ADMIN_USER'..."
PGPASSWORD="$ADMIN_USER" psql -U "$ADMIN_USER" -h localhost -d "$TARGET_DB_NAME" < "$BACKUP_FILE"

if [ $? -ne 0 ]; then
    echo "❌ ERROR: psql restore failed. Check password or connection settings for '$ADMIN_USER'."
    exit 1
fi
echo "Restoration complete! Database '$TARGET_DB_NAME' is a copy of '$SOURCE_DB_NAME'."