import fs from "fs";
import path from "path";

const tmp = process.env.TEMP || "C:/Users/35163/AppData/Local/Temp";
const xml = fs.readFileSync(path.join(tmp, "docx_w", "word", "document.xml"), "utf8");
const reDrawing = /<w:drawing[\s\S]*?<\/w:drawing>/g;
let idx = 0;
let d;
while ((d = reDrawing.exec(xml)) !== null) {
  idx++;
  const start = Math.max(0, d.index - 1200);
  const ctx = xml.slice(start, d.index);
  const tMatch = [...ctx.matchAll(/<w:t[^>]*>([^<]*)<\/w:t>/g)];
  const texts = tMatch.slice(-8).map((x) => x[1]);
  console.log("\n--- drawing", idx, "last w:t:", texts.join(" | "));
}
