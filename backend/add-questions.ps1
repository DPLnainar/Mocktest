# Add Sample Questions to Test ID 14 (Using Docker)
# This script uses Docker to execute SQL without needing psql installed locally

Write-Host "Adding sample questions to Test ID 14..." -ForegroundColor Cyan

# Check if Docker is running
try {
    $dockerCheck = docker ps 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Docker is not running. Please start Docker Desktop." -ForegroundColor Red
        exit 1
    }
}
catch {
    Write-Host "❌ Docker is not available. Please install Docker Desktop." -ForegroundColor Red
    exit 1
}

# Check if postgres container is running
$containerName = "exam-portal-postgres"
$containerRunning = docker ps --filter "name=$containerName" --format "{{.Names}}" 2>&1

if ($containerRunning -ne $containerName) {
    Write-Host "❌ PostgreSQL container is not running." -ForegroundColor Red
    Write-Host "Please start the containers with: docker-compose up -d" -ForegroundColor Yellow
    exit 1
}

Write-Host "Executing SQL script using Docker..." -ForegroundColor Yellow

try {
    # Copy SQL file to container
    docker cp "add_sample_questions_test14.sql" "${containerName}:/tmp/add_questions.sql" 2>&1 | Out-Null
    
    # Execute SQL script inside container
    $result = docker exec -i $containerName psql -U exam_admin -d exam_portal_db -f /tmp/add_questions.sql 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "`n✅ Successfully added questions to Test ID 14!" -ForegroundColor Green
        Write-Host "`nQuestions added:" -ForegroundColor Cyan
        Write-Host "  - 5 MCQ questions (2 marks each)" -ForegroundColor White
        Write-Host "    1. Java boolean default value" -ForegroundColor Gray
        Write-Host "    2. LIFO data structure" -ForegroundColor Gray
        Write-Host "    3. Binary search complexity" -ForegroundColor Gray
        Write-Host "    4. OOP pillars" -ForegroundColor Gray
        Write-Host "    5. SQL SELECT command" -ForegroundColor Gray
        Write-Host "`n  - 2 Coding questions (5 marks each)" -ForegroundColor White
        Write-Host "    1. Sum of two numbers" -ForegroundColor Gray
        Write-Host "    2. Reverse a string" -ForegroundColor Gray
        Write-Host "`nTotal: 7 questions, 20 marks" -ForegroundColor White
        Write-Host "`n🎉 You can now test the application with Test ID 14!" -ForegroundColor Green
        
        # Clean up temp file
        docker exec -i $containerName rm /tmp/add_questions.sql 2>&1 | Out-Null
    }
    else {
        Write-Host "`n❌ Error executing SQL script:" -ForegroundColor Red
        Write-Host $result -ForegroundColor Red
    }
}
catch {
    Write-Host "`n❌ Error: $_" -ForegroundColor Red
}

Write-Host "`nPress any key to continue..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
