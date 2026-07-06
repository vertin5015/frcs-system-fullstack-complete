import fs from "fs";
import path from "path";

const tmp = process.env.TEMP || "C:/Users/35163/AppData/Local/Temp";
const xml = fs.readFileSync(path.join(tmp, "docx_w", "word", "document.xml"), "utf8");

const tokens = [
  "图3.1",
  "图3.2",
  "图3.3",
  "图4.1",
  "图4.2",
  "图4.3",
  "图4.4",
  "图C-1",
  "图C-2",
  "图C-3",
  "图C-4",
  "图C-5",
  "图C-6",
  "图C-7",
  "图C-8",
];
for (const t of tokens) {
  const i = xml.indexOf(t);
  console.log(t, i >= 0 ? i : "NOT FOUND");
}

const embeds = [];
for (const m of xml.matchAll(/r:embed="(rId\d+)"/g)) {
  embeds.push({ id: m[1], pos: m.index });
}
console.log("\nembed count", embeds.length);
embeds.forEach((e, n) => console.log(n + 1, e.id, "pos", e.pos));

const reTok = new RegExp(
  tokens.map((t) => t.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")).join("|"),
  "g",
);
console.log("\n--- last caption token in 8000 chars before each embed");
for (const e of embeds) {
  const back = xml.slice(Math.max(0, e.pos - 8000), e.pos);
  let last;
  let m;
  while ((m = reTok.exec(back)) !== null) last = m[0];
  console.log(e.id, last ?? "(none)");
}
