# Environment Variables Reference

| Variable Name | Default Value | Description |
|---|---|---|
| `SERVER_PORT` | `8080` | HTTP port for Spring Boot backend |
| `SUPABASE_DB_URL` | *(None / H2-PG Fallback)* | Supabase PostgreSQL JDBC connection string with `sslmode=require` |
| `SUPABASE_DB_USER` | `postgres` | Database username |
| `SUPABASE_DB_PASSWORD` | *(None)* | Database password |
| `JWT_SECRET` | `projectflow-dev-secret-key-32-bytes-minimum!!` | 256-bit secret used for HMAC-SHA256 JWT signature verification |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Whitelisted cross-origin origins |
| `VITE_API_URL` | `http://localhost:8080/api/v1` | Base API URL targeted by React Admin Panel |
