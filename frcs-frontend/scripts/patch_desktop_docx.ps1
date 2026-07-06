# Patches Desktop "全文-排版后.docx" via Word COM.
# Requires: Microsoft Word; PNG files in $assets (run: node E:\frcs-frontend\scripts\svg_to_png_for_word.mjs)
$ErrorActionPreference = "Stop"
$docPath = "C:\Users\35163\Desktop\涉外法律知识查询与问答系统_论文初稿_全文-排版后.docx"
$assets  = "C:\Users\35163\Desktop\thesis_word_assets"

if (-not (Test-Path $docPath)) { throw "Docx not found: $docPath" }
if (-not (Test-Path $assets)) { throw "Run first: node E:\frcs-frontend\scripts\svg_to_png_for_word.mjs" }

$bak = "$docPath.bak_" + (Get-Date -Format "yyyyMMddHHmmss")
Copy-Item -LiteralPath $docPath -Destination $bak -Force
Write-Host "Backup: $bak"

Stop-Process -Name "WINWORD" -Force -ErrorAction SilentlyContinue
Start-Sleep -Milliseconds 400

$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0
$doc = $word.Documents.Open($docPath, $false, $false, $false)

function Replace-Once([string]$from, [string]$to) {
  $rng = $doc.Content
  $f = $rng.Find
  $f.ClearFormatting()
  $f.Replacement.ClearFormatting()
  $n = 0
  while ($true) {
    $rng = $doc.Content
    $f = $rng.Find
    $f.Text = $from
    $f.Forward = $true
    $f.Wrap = 0
    if (-not $f.Execute()) { break }
    $rng.Text = $to
    $n++
  }
  if ($n -gt 0) { Write-Host "Replace ($n): $($from.Substring(0,[Math]::Min(40,$from.Length)))..." }
}

# wdParagraph = 4 ; wdCollapseEnd = 0
function InsertPicturesAfterParagraphContaining([string]$anchor, [string[]]$pngFiles) {
  $wdCollapseEnd = 0
  $rng = $doc.Content
  $f = $rng.Find
  $f.Text = $anchor
  $f.Forward = $true
  $f.Wrap = 1
  if (-not $f.Execute()) { Write-Warning "Anchor not found: $anchor"; return }
  $sel = $word.Selection
  $paraRng = $sel.Range.Paragraphs(1).Range.Duplicate
  $paraRng.Collapse($wdCollapseEnd)
  $paraRng.Select()
  foreach ($png in $pngFiles) {
    if (-not (Test-Path $png)) { Write-Warning "Missing PNG: $png"; continue }
    $sel.InsertParagraphAfter()
    try {
      $pic = $sel.InlineShapes.AddPicture($png, $false, $true)
      $pic.LockAspectRatio = $true
      if ($pic.Width -gt 480) { $pic.Width = 480 }
      $sel.SetRange($pic.Range.End, $pic.Range.End)
    } catch { Write-Warning "AddPicture failed: $png $_" }
  }
}

# --- Text fixes (plan: placeholders, abstract entities, TOC note via 插图 index, ch6 cross-ref) ---
Replace-Once "图3.1（系统业务流程图，占位）" "图3.1"
Replace-Once "图3.2（用例图，占位）" "图3.2"
Replace-Once "图3.3（案例检索活动图，占位）" "图3.3"
Replace-Once "图4.1 系统总体架构图（占位）" "图4.1 系统总体架构图"
Replace-Once "图4.2 功能模块结构图（占位）" "图4.2 功能模块结构图"
Replace-Once "占位：时序图、ER 图。" "图4.3 异步摘要时序图；图4.4 核心实体 ER 图（示意）。终稿可改为 Visio/PlantUML 高清导出。"
Replace-Once "截图与详尽步骤可整理入附录C以满足学院对佐证材料的要求。" "界面佐证见附录 C（图 C-1—图 C-8）；占位图请在定稿前替换为实机截屏并做题注与交叉引用。"
Replace-Once "China&apos;s" "China's"
Replace-Once "users&apos;" "users'"

# Optional table paragraph in appendix B — replace with short lead-in + table will be inserted manually if COM table fails; we replace with expanded text for 实测 columns
Replace-Once "可与表6.1 扩充列：实测结果、备注、截图编号。装订前建议使用学校三线表样式重新排版。" "下表在表6.1基础上增加实测结果、测试日期与截图编号列（节选）。装订前请用学校三线表重排。"

# Metrics blurb before 参考文献 (insert only if not present)
if ($doc.Content.Text -notmatch "稿面字数与页数核对说明") {
  $rng = $doc.Content
  $f = $rng.Find
  $f.Text = "与第1章提出的研究目标首尾呼应。"
  $f.Forward = $true
  $f.Wrap = 1
  if ($f.Execute()) {
    $ins = "与第1章提出的研究目标首尾呼应。`r`n`r`n稿面字数与页数核对说明：请在本机 Word 使用审阅→字数统计核对正文与第7章；全文页数以打印预览为准。仓库脚本 thesis_assets/thesis_metrics.mjs 可从 HTML 同步稿生成 thesis_metrics_report.json 作为对照。`r`n`r`n"
    $rng.InsertAfter($ins)
  }
}

# --- Figures (top to bottom) ---
InsertPicturesAfterParagraphContaining "充值流程在配额不足时提示用户跳转支付或模拟结账。" @(
  (Join-Path $assets "fig3_1_business_flow.png"))
InsertPicturesAfterParagraphContaining "注册用户与游客在案例检索上的继承关系、与管理员的授权关系均应体现。" @(
  (Join-Path $assets "fig3_2_use_case.png"))
InsertPicturesAfterParagraphContaining "逐项引用以保持学院格式一致。" @(
  (Join-Path $assets "fig3_3_activity_search.png"))
InsertPicturesAfterParagraphContaining "形成简化版 RAG 流水线[11][17]。" @(
  (Join-Path $assets "fig4_1_architecture.png"))
InsertPicturesAfterParagraphContaining "（提示词模板与日志）。" @(
  (Join-Path $assets "fig4_2_modules.png"))
InsertPicturesAfterParagraphContaining "并在图中标明外键及唯一约束。" @(
  (Join-Path $assets "fig_optional_sequence.png"),
  (Join-Path $assets "fig_optional_er.png"))

# Appendix C PNGs
$cFiles = @(
  "appendix_c_01_login.png",
  "appendix_c_02_search.png",
  "appendix_c_03_list.png",
  "appendix_c_04_reader.png",
  "appendix_c_05_summary.png",
  "appendix_c_06_pay.png",
  "appendix_c_07_favorites.png",
  "appendix_c_08_kb.png"
) | ForEach-Object { Join-Path $assets $_ }

InsertPicturesAfterParagraphContaining "并按章编号在正文交叉引用。" $cFiles

try {
  $doc.Save()
} finally {
  if ($null -ne $doc) { $doc.Close([ref]$false) }
  if ($null -ne $word) {
    $word.Quit([ref]$false)
    [System.Runtime.InteropServices.Marshal]::ReleaseComObject($word) | Out-Null
  }
  [GC]::Collect()
}
Write-Host "Done. Saved: $docPath"
