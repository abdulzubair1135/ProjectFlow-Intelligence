# Backup and Disaster Recovery Strategy

## Central Database Backup (Supabase / PostgreSQL)
1. **Continuous WAL Archiving**: Point-In-Time-Recovery (PITR) enabled on Supabase PostgreSQL with retention up to 7 days.
2. **Scheduled Logical Dumps**: Nightly logical backups executed via `pg_dump`:
   ```bash
   pg_dump "$SUPABASE_DB_URL" -F c -b -v -f "projectflow_backup_$(date +%Y%m%d).dump"
   ```
3. **Restoration Verification**: Monthly automated restore rehearsal verifying table row count and Flyway version integrity.

## Local Client Cache Recovery
- If a client device is wiped or uninstalled, the user simply re-authenticates.
- The `SyncService.pull()` endpoint downloads the complete workspace state since genesis, rebuilding the local SQLite database automatically.
