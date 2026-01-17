# Exam Portal - Backend Startup Script
# This script starts all Docker containers for the backend

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Exam Portal - Starting Backend" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if Docker is running
Write-Host "Checking Docker status..." -ForegroundColor Yellow
docker info 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Docker is not running!" -ForegroundColor Red
    Write-Host "Please start Docker Desktop and try again." -ForegroundColor Red
    exit 1
}
Write-Host "✓ Docker is running" -ForegroundColor Green
Write-Host ""

# Navigate to backend directory
Set-Location -Path "$PSScriptRoot"

# Start all containers
Write-Host "Starting all containers..." -ForegroundColor Yellow
docker-compose up -d

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "  Backend Started Successfully!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Services running:" -ForegroundColor Cyan
    Write-Host "  • Backend API:    http://localhost:8080" -ForegroundColor White
    Write-Host "  • PostgreSQL:     localhost:5432" -ForegroundColor White
    Write-Host "  • Redis:          localhost:6379" -ForegroundColor White
    Write-Host "  • RabbitMQ UI:    http://localhost:15672" -ForegroundColor White
    Write-Host "  • Judge0:         http://localhost:2358" -ForegroundColor White
    Write-Host ""
    Write-Host "To view logs: docker-compose logs -f" -ForegroundColor Gray
    Write-Host "To stop: docker-compose down" -ForegroundColor Gray
}
else {
    Write-Host ""
    Write-Host "ERROR: Failed to start containers!" -ForegroundColor Red
    Write-Host "Check the error messages above." -ForegroundColor Red
    exit 1
}
