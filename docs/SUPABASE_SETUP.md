# Supabase PostgreSQL Integration Guide

ProjectFlow Intelligence natively supports **Supabase PostgreSQL** as its enterprise production database backend, while providing an automatic fallback to local **H2 PostgreSQL compatibility mode** when environment variables are omitted.

---

## Configuration via Environment Variables

To connect ProjectFlow backend to a Supabase project, supply standard database credentials:

```bash
# JDBC Connection URL from Supabase Database Settings (Port 5432 or 6543 for PgBouncer)
export SUPABASE_DB_URL="jdbc:postgresql://db.<PROJECT-REF>.supabase.co:5432/postgres?sslmode=require"

# Supabase database user (usually 'postgres')
export SUPABASE_DB_USER="postgres"

# Supabase database password
export SUPABASE_DB_PASSWORD="your-strong-supabase-password"
```

---

## Automatic Schema Migration
Upon startup, the Spring Boot application runs **Flyway 10**:
1. Checks the `flyway_schema_history` table on the remote Supabase database.
2. Applies `V1__init_schema.sql` cleanly within a single transaction.
3. Automatically provisions all 19 normalized tables, indexes, and foreign keys.

---

## Development Mode Fallback
If `SUPABASE_DB_URL` is omitted, the backend automatically initializes an in-memory or file-backed **H2 Database configured in PostgreSQL compatibility mode** (`MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH`).
This enables instant test execution, continuous integration, and local evaluation without third-party network dependencies.
