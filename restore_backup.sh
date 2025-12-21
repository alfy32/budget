#!/bin/bash

# --- Configuration ---
ADMIN_USER="postgres"        # User with CREATEDB/DROPDB privileges
ADMIN_DB_PASS="postgres"     # The password for the ADMIN_USER (used for creation/dropping)

TARGET_DB_NAME="budget_prod"
BACKUP_DIR="/home/alan/budget-backups"

# --- 1. Selection Logic ---
echo "📂 Scanning for backups in $BACKUP_DIR..."

# Check if directory exists
if [ ! -d "$BACKUP_DIR" ]; then
    echo "❌ ERROR: Backup directory $BACKUP_DIR does not exist."
    exit 1
fi

# List .sql files and store them in an array
files=($(ls "$BACKUP_DIR"/*.sql 2>/dev/null))

if [ ${#files[@]} -eq 0 ]; then
    echo "❌ No .sql backup files found in $BACKUP_DIR."
    exit 1
fi

echo "Available backups:"
for i in "${!files[@]}"; do
    echo "[$i] $(basename "${files[$i]}")"
done

read -p "Enter the number of the file you want to restore: " selection

# Validate selection
if [[ ! "$selection" =~ ^[0-9]+$ ]] || [ "$selection" -ge "${#files[@]}" ]; then
    echo "❌ Invalid selection. Exiting."
    exit 1
fi

BACKUP_FILE="${files[$selection]}"
echo "✅ Selected: $(basename "$BACKUP_FILE")"

# --- 2. Administrative Operations (Drop and Create) ---

echo "🔍 Checking for existing target database '$TARGET_DB_NAME'..."
DB_EXISTS=$(PGPASSWORD="$ADMIN_DB_PASS" psql -U "$ADMIN_USER" -h localhost -lqt | cut -d \| -f 1 | grep -w "$TARGET_DB_NAME")

if [ -n "$DB_EXISTS" ]; then
    echo "🚨 Target database '$TARGET_DB_NAME' found. Dropping it..."
    PGPASSWORD="$ADMIN_DB_PASS" dropdb -U "$ADMIN_USER" -h localhost "$TARGET_DB_NAME" 2> /dev/null
    if [ $? -ne 0 ]; then
        echo "❌ ERROR: Could not drop existing database '$TARGET_DB_NAME'."
        exit 1
    fi
fi

echo "⚙️ Creating new database '$TARGET_DB_NAME'..."
PGPASSWORD="$ADMIN_DB_PASS" createdb -U "$ADMIN_USER" -h localhost -O "$ADMIN_USER" "$TARGET_DB_NAME"

if [ $? -ne 0 ]; then
    echo "❌ ERROR: createdb failed."
    exit 1
fi

# --- 3. Restore the selected backup file ---
echo "➡️ Restoring $(basename "$BACKUP_FILE") into '$TARGET_DB_NAME'..."
PGPASSWORD="$ADMIN_USER" psql -U "$ADMIN_USER" -h localhost -d "$TARGET_DB_NAME" < "$BACKUP_FILE"

if [ $? -ne 0 ]; then
    echo "❌ ERROR: psql restore failed."
    exit 1
fi

echo "✨ Restoration complete! Database '$TARGET_DB_NAME' has been updated."
