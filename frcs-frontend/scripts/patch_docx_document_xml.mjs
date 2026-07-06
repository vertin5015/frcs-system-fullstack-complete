/**
 * Patch docx: text in word/document.xml (no COM).
 * Usage: node scripts/patch_docx_document_xml.mjs <input.docx> [output.docx]
 * If output omitted, overwrites input (closes Word first if file locked).
 */
import fs from "fs";
import path from "path";
import JSZip from "jszip";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const docPath = process.argv[2] || path.join(process.env.USERPROFILE, "Desktop", "涉外法律知识查询与问答系统_论文初稿_全文-排版后.docx");
const outPath = process.argv[3] || docPath;

if (!fs.existsSync(docPath)) {
  console.error("File not found:", docPath);
  process.exit(1);
}

const bak = `${docPath}.bak_${Date.now()}.docx`;
fs.copyFileSync(docPath, bak);
console.log("Backup:", bak);

const zipBuf = fs.readFileSync(docPath);
const zip = await JSZip.loadAsync(zipBuf);
let xml = await zip.file("word/document.xml").async("string");

const reps = [
  ["图3.1（系统业务流程图，占位）", "图3.1"],
  ["图3.2（用例图，占位）", "图3.2"],
  ["图3.3（案例检索活动图，占位）", "图3.3"],
  ["图4.1 系统总体架构图（占位）", "图4.1 系统总体架构图"],
  ["图4.2 功能模块结构图（占位）", "图4.2 功能模块结构图"],
  ["占位：时序图、ER 图。", "图4.3 异步摘要时序图；图4.4 核心实体 ER 图（示意）。终稿可改为 Visio/PlantUML 高清导出。"],
  ["截图与详尽步骤可整理入附录C以满足学院对佐证材料的要求。", "界面佐证见附录 C（图 C-1—图 C-8）；占位图请在定稿前替换为实机截屏并做题注与交叉引用。"],
  ["China&apos;s", "China's"],
  ["users&apos;", "users'"],
  [
    "可与表6.1 扩充列：实测结果、备注、截图编号。装订前建议使用学校三线表样式重新排版。",
    "下表在表6.1基础上增加实测结果、测试日期与截图编号列（节选）。装订前请用学校三线表重排。"
  ],
];

let n = 0;
for (const [a, b] of reps) {
  const before = xml;
  xml = xml.split(a).join(b);
  if (before !== xml) n++;
}
console.log("Replacement groups applied:", n);

if (!xml.includes("稿面字数与页数核对说明")) {
  const ins =
    '<w:p><w:r><w:rPr><w:rFonts w:hint="eastAsia"/><w:b/><w:sz w:val="24"/></w:rPr>' +
    '<w:t>稿面字数与页数核对说明</w:t></w:r></w:p>' +
    '<w:p><w:r><w:rPr><w:rFonts w:hint="eastAsia"/><w:sz w:val="24"/></w:rPr>' +
    '<w:t xml:space="preserve">请在本机 Word「审阅→字数统计」核对正文与第7章；全文页数以打印预览为准。配套 HTML 稿可用 thesis_assets/thesis_metrics.mjs 生成 thesis_metrics_report.json。插图 PNG 见 thesis_word_assets 文件夹。</w:t></w:r></w:p>';
  const mark = "与第1章提出的研究目标首尾呼应。</w:t></w:r></w:p>";
  if (xml.includes(mark)) {
    xml = xml.replace(mark, mark + ins);
    console.log("Inserted metrics note after 7.4.");
  }
}

zip.file("word/document.xml", xml);
const outBuf = await zip.generateAsync({ type: "nodebuffer", compression: "DEFLATE" });
try {
  fs.writeFileSync(outPath, outBuf);
  console.log("Saved:", outPath);
} catch (e) {
  if (e.code === "EBUSY") {
    const fb = outPath.replace(/\.docx$/i, "_patched.docx");
    const alt = path.join(path.dirname(docPath) === "." ? __dirname + "/.." : path.dirname(docPath), path.basename(fb));
    const fallback = path.join(__dirname, "..", path.basename(docPath).replace(/\.docx$/i, "_patched.docx"));
    fs.writeFileSync(fallback, outBuf);
    console.log("Target locked; wrote:", fallback);
  } else throw e;
}
console.log("Figures: insert PNGs from thesis_word_assets next to anchor paragraphs (see WORD插图步骤.txt on Desktop).");
