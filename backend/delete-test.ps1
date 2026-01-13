# Delete Test Helper Script
# Quick script to delete tests from the database

param(
    [Parameter(Mandatory = $true)]
    [int]$TestId
)

Write-Host "Deleting test with ID: $TestId" -ForegroundColor Yellow

docker exec -it exam-portal-postgres psql -U exam_admin -d exam_portal_db -c "DELETE FROM tests WHERE id = $TestId; SELECT 'Test deleted successfully' as result;" | Out-Null

if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Test $TestId deleted successfully!" -ForegroundColor Green
}
else {
    Write-Host "✗ Failed to delete test $TestId" -ForegroundColor Red
}
