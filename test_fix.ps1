$headers = @{
    "Content-Type"  = "application/json"
    "Authorization" = "Bearer eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJhZG1pbkBleGFtcG9ydGFsLmNvbSIsInJvbGVzIjpbIkFETUlOIl0sImRlcGFydG1lbnQiOiJBRE1JTiIsInVzZXJJZCI6MSwiZW1haWwiOiJhZG1pbiIsImlhdCI6MTc2ODQ3ODA2NywiZXhwIjoxNzY4NTY0NDY3fQ.NgjqaHqqr0x5WIPMU5PhonL7ikW7jg7heT9C78Tcj81ull5IHIq2Gok2PEdE-ZCK"
}

$body = @{
    title           = "Test Empty Verify"
    startDateTime   = "2026-01-15T16:30:00"
    endDateTime     = "2026-01-15T18:00:00"
    durationMinutes = 60
    type            = "CODING_ONLY"
    testQuestions   = @()
} | ConvertTo-Json -Depth 10

try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/tests" -Method Post -Body $body -Headers $headers
}
catch {
    Write-Host "Status Code: $($_.Exception.Response.StatusCode.value__)"
    $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    $reader.ReadToEnd()
}
