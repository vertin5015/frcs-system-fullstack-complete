# 25 search stress test (ASCII keywords only; avoids script file encoding issues on Windows)
$ErrorActionPreference = "Continue"
$base = "http://127.0.0.1:8122/api/cases/search"
$keywords = @(
    "contract", "negligence", "copyright", "trademark", "patent",
    "employment", "fraud", "arbitration", "divorce", "custody",
    "merger", "antitrust", "privacy", "securities", "bankruptcy",
    "insurance", "malpractice", "liability", "pollution", "tax",
    "immigration", "housing", "maritime", "defamation", "kidnapping"
)
$countries = @("", "US", "EU", "JPN")
$results = @()
$i = 0
foreach ($kw in $keywords) {
    $i++
    $c = $countries[$i % $countries.Count]
    $enc = [uri]::EscapeDataString($kw)
    $url = "$base`?userId=0&keyword=$enc&language=zh&pagesize=5"
    if ($c) { $url += "&country=$c" }
    Write-Host "[$i/$($keywords.Count)] country=$c keyword=$kw"
    try {
        $resp = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 120
        $j = $resp.Content | ConvertFrom-Json
        $ok = ($j.code -eq 200)
        $cnt = 0
        if ($ok -and $null -ne $j.data.totalCount) { $cnt = [int]$j.data.totalCount }
        elseif ($ok -and $j.data.cases) { $cnt = @($j.data.cases).Count }
        $results += [pscustomobject]@{ Keyword = $kw; Country = $c; Ok = $ok; Code = $j.code; Msg = $j.message; Total = $cnt }
        if (-not $ok) { Write-Host "  FAIL: $($j.message)" }
    }
    catch {
        $results += [pscustomobject]@{ Keyword = $kw; Country = $c; Ok = $false; Code = -1; Msg = $_.Exception.Message; Total = 0 }
        Write-Host "  EX: $($_.Exception.Message)"
    }
    Start-Sleep -Milliseconds 500
}

Write-Host "`n=== SUMMARY ==="
$results | Format-Table -AutoSize
$bad = @($results | Where-Object { -not $_.Ok })
Write-Host "Failures: $($bad.Count) / $($results.Count)"
if ($bad.Count -gt 0) { $bad | Format-List }
exit $(if ($bad.Count -gt 0) { 1 } else { 0 })
