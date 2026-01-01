#!/bin/bash
# Daily Database Backup
# Usage: ./scripts/backup.sh
# Cron: 0 2 * * * /path/to/scripts/backup.sh >> /var/log/exam-portal-backup.log 2>&1

BACKUP_DIR="./backups/postgres"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
FILENAME="exam_portal_db_$TIMESTAMP.sql.gz"

mkdir -p $BACKUP_DIR

echo "📦 Starting backup for $TIMESTAMP..."

# Dump Postgres Data
docker exec exam-portal-postgres pg_dump -U exam_portal_user exam_portal_db | gzip > "$BACKUP_DIR/$FILENAME"

if [ $? -eq 0 ]; then
    echo "✅ Backup saved to $BACKUP_DIR/$FILENAME"
else
    echo "❌ Backup FAILED!"
    exit 1
fi

# Keep only last 7 days of backups
find $BACKUP_DIR -type f -name "*.sql.gz" -mtime +7 -delete

# Get backup size
BACKUP_SIZE=$(du -h "$BACKUP_DIR/$FILENAME" | cut -f1)
echo "📊 Backup size: $BACKUP_SIZE"

# Optional: Upload to AWS S3
# echo "☁️  Uploading to S3..."
# aws s3 cp "$BACKUP_DIR/$FILENAME" s3://exam-portal-backups/postgres/ --storage-class STANDARD_IA
# if [ $? -eq 0 ]; then
#     echo "✅ S3 upload complete"
# else
#     echo "⚠️  S3 upload failed (backup still saved locally)"
# fi

echo "✅ Backup process complete"
