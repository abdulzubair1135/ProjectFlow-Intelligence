# Error Codes & Handling Reference

All error responses from the backend return structured JSON payloads:
```json
{
  "timestamp": "2026-10-14T22:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Project dependency graph contains circular cycles",
  "path": "/api/v1/projects/123/dependencies"
}
```

---

## Standard Error Matrix

| HTTP Status | Error Type | Typical Cause | Recommended Action |
|---|---|---|---|
| `400 Bad Request` | `ValidationException` | Invalid dates, empty names, or DAG cycle | Verify dependency predecessor IDs |
| `401 Unauthorized` | `UnauthorizedException` | Missing or expired Bearer token | Sign in via `/api/v1/auth/dev-login` |
| `403 Forbidden` | `ForbiddenException` | Insufficient role or cross-tenant query | Verify workspace membership and role |
| `404 Not Found` | `ResourceNotFoundException` | Entity ID does not exist | Check workspace or project UUID |
| `409 Conflict` | `ConflictException` | Optimistic locking version mismatch | Re-pull entity before submitting |
| `500 Server Error` | `InternalServerError` | Uncaught exception | Check Spring Boot logs |
