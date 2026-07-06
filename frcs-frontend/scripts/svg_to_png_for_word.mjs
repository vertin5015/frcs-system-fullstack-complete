/**
 * Rasterize thesis SVGs to PNG for Word 2007+ (desktop docx patch).
 * Usage: node scripts/svg_to_png_for_word.mjs <outDir>
 */
import fs from "fs";
import path from "path";
import sharp from "sharp";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.join(__dirname, "..");
const assetDir = path.join(root, "thesis_assets");
const outDir = process.argv[2] || path.join(process.env.USERPROFILE || "", "Desktop", "thesis_word_assets");

const files = fs.readdirSync(assetDir).filter((f) => f.endsWith(".svg"));

if (!fs.existsSync(outDir)) fs.mkdirSync(outDir, { recursive: true });

for (const f of files) {
  const inPath = path.join(assetDir, f);
  const base = f.replace(/\.svg$/i, ".png");
  const outPath = path.join(outDir, base);
  let s = fs.readFileSync(inPath, "utf8");
  s = s.replace(/[\x00-\x08\x0B\x0C\x0E-\x1F]/g, "");
  const buf = Buffer.from(s, "utf8");
  await sharp(buf, { density: 150 })
    .png()
    .resize({ width: 1400, height: 1200, fit: "inside", withoutEnlargement: true })
    .toFile(outPath);
  console.log("Wrote", outPath);
}
console.log("Done. Output:", outDir);
