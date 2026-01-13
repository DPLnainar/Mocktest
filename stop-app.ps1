# ============================================
# Exam Portal - Stop All Services
# ============================================

$ErrorActionPreference = "Stop"

Write-Host "Stopping Exam Portal..." -ForegroundColor Yellow
Write-Host ""

# Stop Docker containers
Write-Host "→ Stopping backend services..." -ForegroundColor Cyan
Set-Location "$PSScriptRoot\backend"
docker-compose down

Write-Host ""
Write-Host "✓ All services stopped!" -ForegroundColor Green
Write-Host ""
Write-Host "Note: Frontend will stop when you close its terminal window." -ForegroundColor Gray
