# ============================================
# Exam Portal - Complete Startup Script
# One command to start everything!
# ============================================

$ErrorActionPreference = "Stop"

# Colors
function Write-Success { param($msg) Write-Host $msg -ForegroundColor Green }
function Write-Info { param($msg) Write-Host $msg -ForegroundColor Cyan }
function Write-Warning { param($msg) Write-Host $msg -ForegroundColor Yellow }
function Write-Error { param($msg) Write-Host $msg -ForegroundColor Red }

Clear-Host
Write-Info "╔════════════════════════════════════════╗"
Write-Info "║   Exam Portal - Starting Application  ║"
Write-Info "╔════════════════════════════════════════╗"
Write-Host ""

# Step 1: Check Docker
Write-Info "→ Checking Docker Desktop..."
try {
    docker info 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw }
    Write-Success "  ✓ Docker is running"
}
catch {
    Write-Error "  ✗ Docker is not running!"
    Write-Warning "`n  Please start Docker Desktop and run this script again."
    Write-Warning "  Press any key to exit..."
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
    exit 1
}

# Step 2: Start Backend (Docker)
Write-Info "`n→ Starting backend services..."
Set-Location "$PSScriptRoot\backend"

docker-compose up -d 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Error "  ✗ Failed to start Docker containers"
    exit 1
}
Write-Success "  ✓ Docker containers started"

# Step 3: Wait for Backend to be Ready
Write-Info "`n→ Waiting for backend to be ready..."
$maxAttempts = 30
$attempt = 0
$backendReady = $false

while ($attempt -lt $maxAttempts -and -not $backendReady) {
    $attempt++
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -TimeoutSec 2 -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) {
            $backendReady = $true
        }
    }
    catch {
        # Backend not ready yet
    }
    
    if (-not $backendReady) {
        Write-Host "  ⏳ Attempt $attempt/$maxAttempts..." -NoNewline
        Start-Sleep -Seconds 2
        Write-Host "`r" -NoNewline
    }
}

if ($backendReady) {
    Write-Success "  ✓ Backend is ready!"
}
else {
    Write-Warning "  ⚠ Backend is taking longer than expected"
    Write-Warning "  Continuing anyway... Check logs if issues occur."
}

# Step 4: Start Frontend
Write-Info "`n→ Starting frontend..."
Set-Location "$PSScriptRoot\frontend"

# Check if node_modules exists
if (-not (Test-Path "node_modules")) {
    Write-Warning "  ⚠ Dependencies not installed. Running npm install..."
    npm install
}

# Start frontend in a new window
Write-Success "  ✓ Launching frontend server..."
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot\frontend'; npm run dev"

# Step 5: Summary
Start-Sleep -Seconds 2
Clear-Host
Write-Success "╔════════════════════════════════════════╗"
Write-Success "║     Application Started Successfully!  ║"
Write-Success "╚════════════════════════════════════════╝"
Write-Host ""
Write-Info "Services Running:"
Write-Host "  🌐 Frontend:  " -NoNewline; Write-Success "http://localhost:5173"
Write-Host "  🔧 Backend:   " -NoNewline; Write-Success "http://localhost:8080"
Write-Host "  🗄️  Database:  " -NoNewline; Write-Success "PostgreSQL on port 5432"
Write-Host ""
Write-Info "Quick Commands:"
Write-Host "  • View backend logs:  " -NoNewline; Write-Host "docker logs exam-portal-backend -f" -ForegroundColor Gray
Write-Host "  • Stop everything:    " -NoNewline; Write-Host ".\stop-app.ps1" -ForegroundColor Gray
Write-Host ""
Write-Warning "Opening browser in 3 seconds..."
Start-Sleep -Seconds 3

# Open browser
Start-Process "http://localhost:5173"

Write-Success "`n✨ Ready to use! Happy testing!"
Write-Host ""
