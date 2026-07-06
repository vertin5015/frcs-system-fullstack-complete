$ErrorActionPreference = "Continue"
Add-Type -AssemblyName System.Web

$BaseUrl = "http://localhost:8122/api"
$UserId = 1
$TargetPerGroup = 1000
$PageSize = 10
$MaxPagesPerKeyword = 4
$PollSeconds = 5
$SummaryTimeoutSeconds = 210

$Root = Join-Path (Get-Location) "batch-results"
New-Item -ItemType Directory -Path $Root -Force | Out-Null
$Stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$LogFile = Join-Path $Root "summary-kb-batch-$Stamp.log"
$CsvFile = Join-Path $Root "summary-kb-batch-$Stamp.csv"
$JsonFile = Join-Path $Root "summary-kb-batch-$Stamp.json"

function U([string]$Hex) {
  $chars = @()
  foreach ($h in ($Hex -split " ")) {
    if ($h.Trim() -ne "") {
      $chars += [char]([Convert]::ToInt32($h, 16))
    }
  }
  return -join $chars
}

$ZhKeywords = @(
  (U "5408 540c"),(U "4fb5 6743"),(U "79bb 5a5a"),(U "8bc8 9a97"),(U "4e13 5229"),
  (U "5546 6807"),(U "7248 6743"),(U "52b3 52a8"),(U "7834 4ea7"),(U "8bc1 5238"),
  (U "79fb 6c11"),(U "7a0e 52a1"),(U "4fdd 9669"),(U "503a 52a1"),(U "9057 4ea7"),
  (U "79df 8d41"),(U "629a 517b"),(U "6740 4eba"),(U "6bd2 54c1"),(U "4ea4 901a"),
  (U "533b 7597"),(U "73af 5883"),(U "6d88 8d39 8005"),(U "9690 79c1"),(U "6570 636e"),
  (U "7f51 7edc"),(U "623f 5730 4ea7"),(U "62c5 4fdd"),(U "80a1 4e1c"),(U "516c 53f8")
)

$EnKeywords = @(
  "contract","negligence","fraud","patent","trademark","copyright","employment","bankruptcy","securities","immigration",
  "tax","insurance","antitrust","privacy","criminal","homicide","murder","divorce","custody","tort",
  "product liability","medical malpractice","environmental","consumer protection","loan agreement","shareholder","trade secret","cybersecurity","real estate","lease",
  "probate","inheritance","debt collection","guaranty","partnership","civil rights","free speech","religion","election","police",
  "prisoner","drug trafficking","sentencing","appeal","administrative","procurement","maritime","international trade","discrimination","workers compensation"
)

function Write-Log([string]$Message) {
  $line = "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') $Message"
  Add-Content -Path $LogFile -Value $line -Encoding UTF8
  Write-Output $line
}

function Invoke-ApiGet([string]$Path, [hashtable]$Params, [int]$TimeoutSec = 90) {
  $query = [System.Web.HttpUtility]::ParseQueryString("")
  foreach ($key in $Params.Keys) {
    if ($null -ne $Params[$key] -and "$($Params[$key])" -ne "") {
      $query[$key] = "$($Params[$key])"
    }
  }
  $uri = "$BaseUrl$Path`?$($query.ToString())"
  return Invoke-RestMethod -Uri $uri -TimeoutSec $TimeoutSec
}

function Add-CaseCandidates([string]$Group, [array]$Keywords, [hashtable]$Cases) {
  foreach ($kw in $Keywords) {
    if ($Cases.Count -ge $TargetPerGroup) { break }
    for ($page = 1; $page -le $MaxPagesPerKeyword; $page++) {
      if ($Cases.Count -ge $TargetPerGroup) { break }
      try {
        Write-Log "SEARCH group=$Group page=$page current=$($Cases.Count)/$TargetPerGroup"
        $res = Invoke-ApiGet "/cases/search" @{
          userId = $UserId
          keyword = $kw
          language = "zh"
          country = ""
          period = ""
          sources = "US"
          pagenum = $page
          pagesize = $PageSize
        } 150
        if ($res.code -ne 200 -or $null -eq $res.data -or $null -eq $res.data.cases) {
          Write-Log "SEARCH_FAIL group=$Group page=$page message=$($res.message)"
          continue
        }
        foreach ($c in $res.data.cases) {
          if ($Cases.Count -ge $TargetPerGroup) { break }
          $caseId = "$($c.case_id)".Trim()
          if ($caseId -eq "") { continue }
          if (-not $Cases.ContainsKey($caseId)) {
            $Cases[$caseId] = [ordered]@{
              group = $Group
              keyword = $kw
              caseId = $caseId
              caseName = "$($c.case_name)"
              country = "$($c.country)"
              originalUrl = "$($c.original_document_url)"
            }
            Write-Log "FOUND group=$Group count=$($Cases.Count) caseId=$caseId"
          }
        }
      } catch {
        Write-Log "SEARCH_ERROR group=$Group page=$page error=$($_.Exception.Message)"
      }
      Start-Sleep -Milliseconds 600
    }
  }
}

function Run-Summary([hashtable]$Case) {
  $caseId = $Case.caseId
  $sw = [System.Diagnostics.Stopwatch]::StartNew()
  $status = "UNKNOWN"
  $contentLen = 0
  $err = ""
  try {
    Write-Log "SUMMARY_START group=$($Case.group) caseId=$caseId"
    $start = Invoke-ApiGet "/cases/summaryAsync/start" @{
      userId = $UserId
      caseId = $caseId
      language = "zh"
      force = "false"
    } 60
    if ($start.code -ne 200) {
      $status = "START_FAILED"
      $err = "$($start.message)"
      Write-Log "SUMMARY_START_FAILED caseId=$caseId message=$err"
    } else {
      $deadline = (Get-Date).AddSeconds($SummaryTimeoutSeconds)
      while ((Get-Date) -lt $deadline) {
        Start-Sleep -Seconds $PollSeconds
        $st = Invoke-ApiGet "/cases/summaryAsync/status" @{
          caseId = $caseId
          language = "zh"
        } 60
        if ($st.code -ne 200) {
          $status = "STATUS_FAILED"
          $err = "$($st.message)"
          break
        }
        $status = "$($st.data.status)"
        $content = "$($st.data.content)"
        $contentLen = $content.Length
        Write-Log "SUMMARY_POLL caseId=$caseId status=$status len=$contentLen elapsedMs=$($sw.ElapsedMilliseconds)"
        if ($status -eq "DONE" -or $status -eq "FAILED") {
          if ($status -eq "FAILED") { $err = "$($st.data.errorMessage)" }
          break
        }
      }
      if ($status -eq "RUNNING") {
        $status = "TIMEOUT"
        $err = "summary timeout after $SummaryTimeoutSeconds seconds"
      }
    }
  } catch {
    $status = "ERROR"
    $err = $_.Exception.Message
    Write-Log "SUMMARY_ERROR caseId=$caseId error=$err"
  }
  $sw.Stop()
  return [ordered]@{
    group = $Case.group
    keyword = $Case.keyword
    caseId = $caseId
    caseName = $Case.caseName
    country = $Case.country
    status = $status
    elapsedMs = $sw.ElapsedMilliseconds
    summaryChars = $contentLen
    originalUrl = $Case.originalUrl
    error = $err
  }
}

"group,keyword,caseId,caseName,country,status,elapsedMs,summaryChars,originalUrl,error" | Set-Content -Path $CsvFile -Encoding UTF8
Write-Log "BATCH_START targetPerGroup=$TargetPerGroup userId=$UserId"

$zhCases = @{}
$enCases = @{}
Add-CaseCandidates "zh" $ZhKeywords $zhCases
Add-CaseCandidates "en" $EnKeywords $enCases

$allCases = @()
$allCases += $zhCases.Values | Select-Object -First $TargetPerGroup
$allCases += $enCases.Values | Select-Object -First $TargetPerGroup

Write-Log "COLLECT_DONE zh=$($zhCases.Count) en=$($enCases.Count) totalToSummarize=$($allCases.Count)"

$results = @()
$i = 0
foreach ($case in $allCases) {
  $i++
  Write-Log "QUEUE_PROGRESS $i/$($allCases.Count)"
  $r = Run-Summary $case
  $results += [pscustomobject]$r
  $csvLine = '"{0}","{1}","{2}","{3}","{4}","{5}",{6},{7},"{8}","{9}"' -f `
    ($r.group -replace '"','""'),($r.keyword -replace '"','""'),($r.caseId -replace '"','""'),($r.caseName -replace '"','""'),($r.country -replace '"','""'),($r.status -replace '"','""'),$r.elapsedMs,$r.summaryChars,($r.originalUrl -replace '"','""'),($r.error -replace '"','""')
  Add-Content -Path $CsvFile -Value $csvLine -Encoding UTF8
  $results | ConvertTo-Json -Depth 5 | Set-Content -Path $JsonFile -Encoding UTF8
}

$done = ($results | Where-Object { $_.status -eq "DONE" }).Count
$failed = $results.Count - $done
Write-Log "BATCH_DONE total=$($results.Count) done=$done failed=$failed csv=$CsvFile json=$JsonFile"
