try {
    $token = Get-Content -Path "C:\Users\ganes\OneDrive\Desktop\mock test\admin_token.txt" -ErrorAction SilentlyContinue
    $headers = @{}
    if ($token) {
        $headers["Authorization"] = "Bearer $token"
    }
    $r = Invoke-WebRequest -Uri "http://localhost:8080/api/questions/bulk-create" -Method Post -ContentType "application/json" -InFile "C:\Users\ganes\OneDrive\Desktop\mock test\test_mcq.json" -Headers $headers -ErrorAction Stop
    $r.Content
}
catch {
    if ($_.Exception.Response) {
        $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
        $errContent = $reader.ReadToEnd()
        Write-Output "ERROR_RESPONSE_START"
        Write-Output $errContent
        Write-Output "ERROR_RESPONSE_END"
        $errContent | Out-File -FilePath "last_error.json" -Encoding ascii
    }
    else {
        Write-Output "No Response: $($_.Exception.Message)"
    }
}
