/**
 * 将 thesis_assets/plantuml/out 下最新 PNG 写入 Word OOXML 的 word/media/image1.png…image7.png
 * （与「插图顺序」一致：图3.1–3.3、图4.1–4.4），不改动题注与排版。
 * 附录 C 截图仍为 image8–image15，本脚本不替换。
 *
 * 用法: node scripts/replace_docx_plantuml_media.mjs [输入.docx] [输出.docx]
 */
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import { execSync } from "child_process";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.join(__dirname, "..");

const defaultIn =
  "C:/Users/35163/Desktop/涉外法律知识查询与问答系统_论文初稿_全文-排版后.docx";
const defaultOut = path.join(
  "C:/Users/35163/Desktop",
  "涉外法律知识查询与问答系统_论文初稿_全文-排版后_plantuml.docx",
);

const inFile = process.argv[2] || defaultIn;
const outFile = process.argv[3] || defaultOut;

const outDir = path.join(root, "thesis_assets", "plantuml", "out");
const map = [
  ["image1.png", "fig3_1_business_flow.png"],
  ["image2.png", "fig3_2_use_case.png"],
  ["image3.png", "fig3_3_activity_search.png"],
  ["image4.png", "fig4_1_architecture.png"],
  ["image5.png", "fig4_2_modules.png"],
  ["image6.png", "fig4_3_sequence_async_summary.png"],
  ["image7.png", "fig4_4_er.png"],
];

for (const [, src] of map) {
  const p = path.join(outDir, src);
  if (!fs.existsSync(p)) {
    console.error("缺少导出文件，请先执行 thesis_assets/plantuml/generate.bat（或 java -jar plantuml.jar -tpng …）:", p);
    process.exit(1);
  }
}

if (!fs.existsSync(inFile)) {
  console.error("输入文件不存在:", inFile);
  process.exit(1);
}

const tmp = path.join(
  process.env.TEMP || "C:/Users/35163/AppData/Local/Temp",
  "docx_plantuml_patch_" + Date.now(),
);
const unzipDir = path.join(tmp, "unz");
fs.mkdirSync(unzipDir, { recursive: true });

const zipPath = path.join(tmp, "work.zip");
fs.copyFileSync(inFile, zipPath);
execSync(
  `powershell -NoProfile -Command "Expand-Archive -LiteralPath '${zipPath.replace(/'/g, "''")}' -DestinationPath '${unzipDir.replace(/'/g, "''")}' -Force"`,
  { stdio: "inherit" },
);

const mediaDir = path.join(unzipDir, "word", "media");
if (!fs.existsSync(mediaDir)) {
  console.error("未找到 word/media，可能 docx 损坏或路径错误:", mediaDir);
  process.exit(1);
}

for (const [destName, srcName] of map) {
  const from = path.join(outDir, srcName);
  const to = path.join(mediaDir, destName);
  if (!fs.existsSync(to)) {
    console.warn("警告: 文档内没有", destName, "仍将写入新文件");
  }
  fs.copyFileSync(from, to);
  console.log("→", destName, "<=", srcName);
}

if (fs.existsSync(outFile)) fs.unlinkSync(outFile);
const esc = (p) => `'${String(p).replace(/'/g, "''")}'`;
execSync(
  [
    "powershell",
    "-NoProfile",
    "-Command",
    `Add-Type -AssemblyName System.IO.Compression.FileSystem; [System.IO.Compression.ZipFile]::CreateFromDirectory(${esc(unzipDir)}, ${esc(outFile)})`,
  ].join(" "),
  { stdio: "inherit" },
);

fs.rmSync(tmp, { recursive: true, force: true });
console.log("完成:", outFile);
