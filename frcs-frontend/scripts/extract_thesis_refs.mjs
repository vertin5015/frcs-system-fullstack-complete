import fs from "fs";
import path from "path";

const doc = path.join(process.env.TEMP, "thesis_refs_check", "doc", "word", "document.xml");
const xml = fs.readFileSync(doc, "utf8");
const buf = [];
for (const m of xml.matchAll(/<w:t[^>]*>([\s\S]*?)<\/w:t>/g)) {
  buf.push(m[1]);
}
const text = buf.join("").replace(/\s+/g, " ").trim();
const positions = [];
let from = 0;
for (;;) {
  const i = text.indexOf("参考文献", from);
  if (i < 0) break;
  positions.push(i);
  from = i + 1;
}
console.log("text length:", text.length, "参考文献 count:", positions.length);
const tail = text.slice(Math.max(0, text.length - 25000));
console.log("\n--- document tail (25000 chars) ---\n");
console.log(tail);
