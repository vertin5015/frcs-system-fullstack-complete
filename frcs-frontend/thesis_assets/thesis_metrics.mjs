/**
 * 论文字数估算：从 HTML 论文稿按章边界提取纯文本并统计（供与 Word「字数统计」对照）。
 * 运行：node thesis_assets/thesis_metrics.mjs
 */
import { readFileSync, writeFileSync } from "fs";
import { fileURLToPath } from "url";
import { dirname, join } from "path";

const __dirname = dirname(fileURLToPath(import.meta.url));
const root = join(__dirname, "..");
const htmlPath = join(root, "涉外法律知识查询与问答系统_论文初稿_全文_同步.doc");

function stripTags(htmlSlice) {
  return htmlSlice
    .replace(/<script[\s\S]*?<\/script>/gi, "")
    .replace(/<style[\s\S]*?<\/style>/gi, "")
    .replace(/<[^>]+>/g, "\n")
    .replace(/&nbsp;/g, " ")
    .replace(/&amp;/g, "&")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&apos;/g, "'");
}

const html = readFileSync(htmlPath, "utf8");

const bodyMatch = html.match(/<h1 id="ch1">[\s\S]*?<h1 id="refs">参考文献<\/h1>/i);
const ch7Match = html.match(/<h1 id="ch7">[\s\S]*?<h1 id="refs">参考文献<\/h1>/i);

const bodyText = bodyMatch ? stripTags(bodyMatch[0]) : stripTags(html);
const ch7Text = ch7Match ? stripTags(ch7Match[0]) : "";

const fullText = stripTags(html).replace(/\s+/g, " ").trim();

function countZhChars(s) {
  return (s.match(/[\u4e00-\u9fff]/g) || []).length;
}

const bodyZh = countZhChars(bodyText);
const ch7Zh = countZhChars(ch7Text);
const fullZh = countZhChars(fullText);

const metrics = {
  generatedAt: new Date().toISOString(),
  sourceFile: htmlPath,
  note:
    "汉字为主要指标；不含 Word 页眉页脚、独立封面文本框等。≥40 页请在 Word「打印预览」核对。终稿请以 Word「审阅 → 字数统计」为准（可按学院要求勾选含文本框、脚注）。",
  fullDocument: {
    chineseChars: fullZh,
    nonSpaceChars: fullText.replace(/\s/g, "").length,
  },
  bodyChapter1to7BeforeRefs: {
    chineseChars: bodyZh,
    nonSpaceChars: bodyText.replace(/\s/g, "").length,
    thresholdMinZh: 20000,
    meetsBodyThreshold: bodyZh >= 20000,
  },
  chapter7Section: {
    chineseChars: ch7Zh,
    thresholdDiscussedMinZh: 1200,
    meetsConclusionDiscussedThreshold: ch7Zh >= 1200,
  },
  wordPagesHint:
    "页数与纸张、行距、图表有关，HTML 无法等价折算；请在 Word 用「页面布局 → 页边距 + 打印预览」确认 ≥40 页。",
};

const outPath = join(__dirname, "thesis_metrics_report.json");
writeFileSync(outPath, JSON.stringify(metrics, null, 2), "utf8");
console.log(JSON.stringify(metrics, null, 2));
console.log("\nWritten:", outPath);
