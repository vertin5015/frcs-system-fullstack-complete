param(
  [int]$TargetTotal = 1000,
  [string]$BaseUrl = "http://localhost:8122/api",
  [int]$UserId = 1,
  [int]$MaxPagesPerKeyword = 6,
  [int]$SummaryTimeoutSeconds = 210
)

$ErrorActionPreference = "Continue"

$Here = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Output "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') STEP 1 EU (zh keywords, randomized order, target=$TargetTotal)"
& powershell.exe -ExecutionPolicy Bypass -NoProfile -File (Join-Path $Here "batch-summary-kb-region.ps1") `
  -Region EU `
  -TargetTotal $TargetTotal `
  -BaseUrl $BaseUrl `
  -UserId $UserId `
  -MaxPagesPerKeyword $MaxPagesPerKeyword `
  -SummaryTimeoutSeconds $SummaryTimeoutSeconds

if ($LASTEXITCODE -ne 0 -and $null -ne $LASTEXITCODE) {
  exit $LASTEXITCODE
}

Write-Output "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') STEP 2 JPN (zh keywords, randomized order, target=$TargetTotal)"
& powershell.exe -ExecutionPolicy Bypass -NoProfile -File (Join-Path $Here "batch-summary-kb-region.ps1") `
  -Region JPN `
  -TargetTotal $TargetTotal `
  -BaseUrl $BaseUrl `
  -UserId $UserId `
  -MaxPagesPerKeyword $MaxPagesPerKeyword `
  -SummaryTimeoutSeconds $SummaryTimeoutSeconds

exit $LASTEXITCODE
