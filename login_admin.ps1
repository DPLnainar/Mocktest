
$body = @{
    email    = "admin@examportal.com"
    password = "admin123"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $body -ContentType "application/json"
    $response.token
    $response.token | Out-File -FilePath "admin_token.txt" -Encoding ascii
}
catch {
    Write-Error $_.Exception.ToString()
    if ($_.Exception.Response) {
        $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
        Write-Output $reader.ReadToEnd()
    }
}
