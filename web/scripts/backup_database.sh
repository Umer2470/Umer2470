#!/bin/bash
# ==============================================================================
# CHOUDHURY POS — AUTOMATED ATOMIC SQLITE BACKUP UTILITY
# Creates zero-downtime, crash-consistent SQLite database backups via WAL checkpoint
# ==============================================================================
set -e

SOURCE_DB="${DB_PATH:-/app/data/pos_central.db}"
if [ ! -f "$SOURCE_DB" ]; then
  # Fallback to local directory if running outside Docker
  SOURCE_DB="./pos_central.db"
fi

BACKUP_DIR="${BACKUP_DIR:-./backups}"
mkdir -p "$BACKUP_DIR"

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/pos_backup_${TIMESTAMP}.db"

echo "=========================================================="
echo "Starting CHOUDHURY POS Database Backup..."
echo "Source DB: $SOURCE_DB"
echo "Target:    $BACKUP_FILE"
echo "=========================================================="

if command -v sqlite3 &> /dev/null; then
  # Perform atomic non-blocking online backup using SQLite VACUUM INTO
  sqlite3 "$SOURCE_DB" "VACUUM INTO '${BACKUP_FILE}';"
else
  # Node fallback if sqlite3 CLI is absent
  node -e "
    const { DatabaseSync } = require('node:sqlite');
    const db = new DatabaseSync('$SOURCE_DB');
    db.exec('PRAGMA wal_checkpoint(TRUNCATE)');
  "
  cp "$SOURCE_DB" "$BACKUP_FILE"
fi

# Optional gzip compression
if command -v gzip &> /dev/null; then
  gzip -k "$BACKUP_FILE"
  echo "Compressed archive created: ${BACKUP_FILE}.gz"
fi

echo "Backup completed successfully!"
ls -lh "${BACKUP_DIR}/" | tail -n 5
