#!/bin/bash
# ==============================================================================
# CHOUDHURY POS — SECURE DATABASE RESTORATION UTILITY
# Validates integrity before restoring database file
# ==============================================================================
set -e

BACKUP_FILE="$1"
TARGET_DB="${DB_PATH:-/app/data/pos_central.db}"

if [ -z "$BACKUP_FILE" ]; then
  echo "Usage: ./restore_database.sh <path_to_backup.db or .db.gz>"
  exit 1
fi

if [ ! -f "$BACKUP_FILE" ]; then
  echo "Error: Backup file '$BACKUP_FILE' does not exist."
  exit 1
fi

echo "=========================================================="
echo "CHOUDHURY POS DATABASE RESTORE"
echo "Backup File: $BACKUP_FILE"
echo "Target DB:   $TARGET_DB"
echo "=========================================================="

TEMP_RESTORE_FILE="/tmp/pos_restore_candidate.db"
rm -f "$TEMP_RESTORE_FILE"

# Handle gzipped files
if [[ "$BACKUP_FILE" == *.gz ]]; then
  echo "Decompressing gzip archive..."
  gzip -dc "$BACKUP_FILE" > "$TEMP_RESTORE_FILE"
else
  cp "$BACKUP_FILE" "$TEMP_RESTORE_FILE"
fi

# Integrity check
echo "Validating SQLite integrity check on backup file..."
INTEGRITY_OK=$(node -e "
  const { DatabaseSync } = require('node:sqlite');
  try {
    const db = new DatabaseSync('$TEMP_RESTORE_FILE');
    const row = db.prepare('PRAGMA integrity_check').get();
    if (row && Object.values(row)[0] === 'ok') {
      console.log('OK');
    } else {
      console.log('FAIL');
    }
  } catch (e) {
    console.log('FAIL');
  }
")

if [ "$INTEGRITY_OK" != "OK" ]; then
  echo "CRITICAL ERROR: Backup file failed SQLite integrity check! Aborting restore."
  rm -f "$TEMP_RESTORE_FILE"
  exit 1
fi

echo "Integrity check PASSED. Creating safety backup of current target DB..."
if [ -f "$TARGET_DB" ]; then
  SAFETY_FILE="${TARGET_DB}.pre_restore_$(date +%s).bak"
  cp "$TARGET_DB" "$SAFETY_FILE"
  echo "Safety copy saved to: $SAFETY_FILE"
fi

# Atomic replacement
cp "$TEMP_RESTORE_FILE" "$TARGET_DB"
rm -f "$TEMP_RESTORE_FILE"
rm -f "${TARGET_DB}-shm" "${TARGET_DB}-wal"

echo "=========================================================="
echo "RESTORE SUCCESSFUL! Target database updated."
echo "=========================================================="
