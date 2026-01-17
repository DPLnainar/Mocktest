try {
    $r = Invoke-WebRequest -Uri "http://localhost:8080/api/questions/debug-deserialization" -Method Post -ContentType "application/json" -InFile "C:\Users\ganes\OneDrive\Desktop\mock test\test_payload.json" -ErrorAction Stop
    $r.Content
}
catch {
    if ($_.Exception.Response) {
        $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
        Write-Output "ERROR_RESPONSE_START"
        Write-Output $reader.ReadToEnd()
        Write-Output "ERROR_RESPONSE_END"
    }
    else {
        Write-Output "No Response: $($_.Exception.Message)"
    }
}
