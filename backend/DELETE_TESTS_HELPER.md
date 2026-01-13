# Test Deletion Helper Script
# Use this script to delete tests from the database until the DELETE endpoint is deployed

## Quick Delete Command

To delete a specific test by ID, run:

```powershell
docker exec -it exam-portal-postgres psql -U exam_admin -d exam_portal_db -c "DELETE FROM tests WHERE id = YOUR_TEST_ID;"
```

## View All Tests

To see all tests:

```powershell
docker exec -it exam-portal-postgres psql -U exam_admin -d exam_portal_db -c "SELECT id, title, status, created_at FROM tests ORDER BY id;"
```

## Delete Multiple Tests

To delete multiple tests at once:

```powershell
docker exec -it exam-portal-postgres psql -U exam_admin -d exam_portal_db -c "DELETE FROM tests WHERE id IN (8, 9, 10);"
```

## Delete All Draft Tests

To delete all tests with DRAFT status:

```powershell
docker exec -it exam-portal-postgres psql -U exam_admin -d exam_portal_db -c "DELETE FROM tests WHERE status = 'DRAFT';"
```

## Examples

### Delete test with ID 8:
```powershell
docker exec -it exam-portal-postgres psql -U exam_admin -d exam_portal_db -c "DELETE FROM tests WHERE id = 8;"
```

### Delete test with ID 9:
```powershell
docker exec -it exam-portal-postgres psql -U exam_admin -d exam_portal_db -c "DELETE FROM tests WHERE id = 9;"
```

## Notes

- The CASCADE delete is handled by the database foreign key constraints
- Related `test_questions` will be automatically deleted
- Related `student_attempts` may need to be deleted first if they exist
- This is a temporary workaround until the DELETE endpoint is successfully deployed

## Database Credentials

- **Host**: localhost (via Docker)
- **Port**: 5432
- **Database**: exam_portal_db
- **User**: exam_admin
- **Password**: SecurePassword123!

## When DELETE Endpoint is Fixed

Once the Maven build issue is resolved and the DELETE endpoint is deployed, you can use the UI delete button normally. The frontend is already configured to call the endpoint.
