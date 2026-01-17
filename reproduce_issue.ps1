$headers = @{
    "Content-Type"  = "application/json"
    "Authorization" = "Bearer $(cat token.txt)" # Placeholder if auth needed, but user seems to be using localhost without explicit auth in previous curl
}

$body = @{
    title           = "Test Debug"
    startDateTime   = "2026-01-15T10:00:00"
    endDateTime     = "2026-01-15T12:00:00"
    durationMinutes = 60
    type            = "CODING_ONLY"
    testQuestions   = @(
        @{
            questionOrder = 1
            question      = @{
                type         = "CODING"
                questionText = "Sample Question"
                marks        = 10
            }
        }
    )
} | ConvertTo-Json -Depth 10

try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/tests" -Method Post -Body $body -ContentType "application/json"
}
catch {
    Write-Host "Status Code: $($_.Exception.Response.StatusCode.value__)"
    $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    $reader.ReadToEnd()
}
