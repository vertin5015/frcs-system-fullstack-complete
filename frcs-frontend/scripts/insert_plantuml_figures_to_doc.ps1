param(
  [string]$DocPath = "E:\涉外法律知识查询与问答系统_论文初稿_全文.doc",
  [string]$PlantUmlOut = "E:\frcs-frontend\thesis_assets\plantuml\out",
  [switch]$OverwriteOriginal = $true
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $DocPath)) { throw "Document not found: $DocPath" }
if (-not (Test-Path -LiteralPath $PlantUmlOut)) { throw "PlantUML output directory not found: $PlantUmlOut" }

$figures = @(
  @{ Anchor = "总体业务主线如图3.1"; Image = "fig3_1_business_flow.png"; Caption = "图3.1 系统业务流程图" },
  @{ Anchor = "角色的主要用例如图3.2"; Image = "fig3_2_use_case.png"; Caption = "图3.2 系统用例图" },
  @{ Anchor = "活动图图3.3"; Image = "fig3_3_activity_search.png"; Caption = "图3.3 案例检索活动图" },
  @{ Anchor = "如图4.1所示"; Image = "fig4_1_architecture.png"; Caption = "图4.1 系统总体架构图" },
  @{ Anchor = "图4.2建议以包名粒度绘制"; Image = "fig4_2_modules.png"; Caption = "图4.2 功能模块结构图" },
  @{ Anchor = "异步摘要接口分为 summaryAsync/start"; Image = "fig4_3_sequence_async_summary.png"; Caption = "图4.3 异步摘要与问答时序图" },
  @{ Anchor = "如需在论文中给出 ER 图"; Image = "fig4_4_er.png"; Caption = "图4.4 数据库实体关系图" }
)

foreach ($figure in $figures) {
  $imagePath = Join-Path $PlantUmlOut $figure.Image
  if (-not (Test-Path -LiteralPath $imagePath)) { throw "Missing PlantUML image: $imagePath" }
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupPath = [System.IO.Path]::ChangeExtension($DocPath, $null) + "_before_plantuml_$timestamp.doc"
$outputPath = if ($OverwriteOriginal) { $DocPath } else { [System.IO.Path]::ChangeExtension($DocPath, $null) + "_plantuml.doc" }
Copy-Item -LiteralPath $DocPath -Destination $backupPath -Force

$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0

$wdCollapseStart = 1
$wdCollapseEnd = 0
$wdFindStop = 0
$wdAlignParagraphCenter = 1
$wdFormatDocument = 0

$doc = $null
try {
  $doc = $word.Documents.Open($DocPath, $false, $false)

  foreach ($figure in $figures) {
    if ($doc.Content.Text.Contains($figure.Caption)) {
      Write-Output "Skip existing caption: $($figure.Caption)"
      continue
    }

    $foundParagraph = $null
    foreach ($paragraph in @($doc.Paragraphs)) {
      if ($paragraph.Range.Text.Contains($figure.Anchor)) {
        $foundParagraph = $paragraph
        break
      }
    }
    if ($null -eq $foundParagraph) { throw "Anchor not found: $($figure.Anchor)" }

    $marker = "@@PLANTUML_" + ([guid]::NewGuid().ToString("N")) + "@@"
    $foundParagraph.Range.InsertAfter("`r$marker`r$($figure.Caption)`r")

    $markerRange = $doc.Content
    $find = $markerRange.Find
    $find.ClearFormatting()
    $find.Text = $marker
    $find.Forward = $true
    $find.Wrap = $wdFindStop
    if (-not $find.Execute()) { throw "Inserted marker not found for: $($figure.Caption)" }

    $markerRange.Text = ""
    $markerRange.Collapse($wdCollapseStart)

    $inlineShape = $doc.InlineShapes.AddPicture((Join-Path $PlantUmlOut $figure.Image), $false, $true, $markerRange)
    $inlineShape.LockAspectRatio = $true
    if ($inlineShape.Width -gt 430) { $inlineShape.Width = 430 }
    $inlineShape.Range.ParagraphFormat.Alignment = $wdAlignParagraphCenter

    $captionRange = $inlineShape.Range
    $captionRange.Collapse($wdCollapseEnd)
    $captionRange.MoveEnd(4, 1) | Out-Null
    $captionRange.ParagraphFormat.Alignment = $wdAlignParagraphCenter

    Write-Output "Inserted: $($figure.Caption) <= $($figure.Image)"
  }

  $doc.SaveAs2($outputPath, $wdFormatDocument)
  $doc.Close($false)
  $doc = $null
  Write-Output "Saved: $outputPath"
  Write-Output "Backup: $backupPath"
} finally {
  if ($null -ne $doc) { try { $doc.Close($false) | Out-Null } catch {} }
  try { $word.Quit() | Out-Null } catch {}
  try { [System.Runtime.InteropServices.Marshal]::ReleaseComObject($word) | Out-Null } catch {}
}