# Exam Portal Startup Script

$ErrorActionPreference = "Stop"

function Write-Success { param($msg) Write-Host $msg -ForegroundColor Green }
function Write-Info { param($msg) Write-Host $msg -ForegroundColor Cyan }
function Write-Warning { param($msg) Write-Host $msg -ForegroundColor Yellow }
function Write-Error { param($msg) Write-Host $msg -ForegroundColor Red }

Clear-Host
Write-Info "Starting Exam Portal Application..."

# Step 1: Check Docker
Write-Info "Checking Docker..."
try {
    docker info 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw }
    Write-Success "Docker is running"
}
catch {
    Write-Error "Docker is not running!"
    Write-Warning "Please start Docker Desktop and try again."
    exit 1
}

# Step 2: Start Backend
Write-Info "Starting backend..."
if (Test-Path "$PSScriptRoot\backend") {
    Set-Location "$PSScriptRoot\backend"
}
else {
    Write-Error "Backend directory not found"
    exit 1
}

# Temporarily allow warnings for docker-compose
$oldPreference = $ErrorActionPreference
$ErrorActionPreference = "Continue"

Write-Host "Running docker-compose..."
docker-compose up -d

if ($LASTEXITCODE -ne 0) {
    $ErrorActionPreference = $oldPreference
    Write-Error "Docker compose failed with exit code $LASTEXITCODE"
    exit 1
}
$ErrorActionPreference = $oldPreference
Write-Success "Backend containers started/checked"

# Step 3: Wait for Backend
Write-Info "Waiting for backend to be ready..."
$maxAttempts = 60
$attempt = 0
$backendReady = $false

while ($attempt -lt $maxAttempts -and -not $backendReady) {
    $attempt++
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -TimeoutSec 2 -ErrorAction Stop
        if ($response.StatusCode -eq 200) {
            $backendReady = $true
        }
    }
    catch {
        # Ignore
    }
    
    if (-not $backendReady) {
        Write-Host "." -NoNewline
        Start-Sleep -Seconds 2
    }
}
Write-Host ""

if ($backendReady) {
    Write-Success "Backend is ready!"
}
else {
    Write-Warning "Backend timeout - check logs."
}

# Step 4: Start Frontend
Write-Info "Starting frontend..."
if (Test-Path "$PSScriptRoot\frontend") {
    Set-Location "$PSScriptRoot\frontend"
}
else {
    Write-Error "Frontend directory not found"
    # Proceed? No.
}

if (-not (Test-Path "node_modules")) {
    Write-Warning "Installing dependencies..."
    npm install
}

Write-Success "Launching frontend..."
try {
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot\frontend'; npm run dev"
}
catch {
    Write-Error "Failed to launch frontend"
}

# Step 5: Summary
Start-Sleep -Seconds 2
Clear-Host
Write-Success "Application Started!"
Write-Info "Frontend: http://localhost:3001"
Write-Info "Backend: http://localhost:8080"

Write-Warning "Opening browser..."
Start-Sleep -Seconds 2

try {
    Start-Process "http://localhost:3001"
}
catch {
    Write-Warning "Could not open browser."
}
