import fs from "fs";
import path from "path";

const tmp = process.env.TEMP || "C:/Users/35163/AppData/Local/Temp";
const base = path.join(tmp, "docx_w");
const xml = fs.readFileSync(path.join(base, "word/document.xml"), "utf8");
const rels = fs.readFileSync(path.join(base, "word/_rels/document.xml.rels"), "utf8");

const idToFile = {};
for (const row of rels.matchAll(/<Relationship[^>]+>/g)) {
  const t = row[0];
  const idM = /Id="(rId\d+)"/.exec(t);
  const tgtM = /Target="media\/([^"]+)"/.exec(t);
  if (idM && tgtM) idToFile[idM[1]] = tgtM[1];
}

const order = [];
const re = /r:embed="(rId\d+)"/g;
let m;
while ((m = re.exec(xml)) !== null) {
  const f = idToFile[m[1]];
  if (f && !order.includes(f)) order.push(f);
}
console.log("First-occurrence embed order:");
order.forEach((f, i) => console.log(i + 1, f));
