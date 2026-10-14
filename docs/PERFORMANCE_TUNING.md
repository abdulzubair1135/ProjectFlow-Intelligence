# Performance Tuning & Optimization

## Database & Indexing Optimizations
1. **Composite Tenant Indices**: Every query filtering by tenant uses composite B-Tree indexes:
   - `idx_projects_workspace_status` on `(workspace_id, status)`
   - `idx_tasks_project_status` on `(project_id, status_id)`
2. **Connection Pooling**: HikariCP is tuned with `maximumPoolSize: 10`, `minimumIdle: 2`, `idleTimeout: 30000ms`, preventing connection exhaustion on Supabase PgBouncer limits.
3. **Optimistic Locking**: Handled via `version` column increments, avoiding heavy pessimistic row locks during high-concurrency mobile updates.

## Frontend & Desktop Memory Optimization
- **Vite Bundling**: Code splitting with vendor chunk isolation keeps initial JS payload under 260 kB.
- **JavaFX Table Virtualization**: TableView recycling reuses cell visual nodes, maintaining 60 FPS scrolling on large task sets.
- **Room SQLite Query Paging**: Android recyclerview uses indexed cursor reads for instant scrolling without memory spikes.
