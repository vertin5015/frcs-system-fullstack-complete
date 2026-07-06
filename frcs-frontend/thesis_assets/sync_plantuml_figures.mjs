/**
 * 将 plantuml/out 中最新导出同步到 thesis_assets/，并写 optional 图别名。
 * 运行：node thesis_assets/sync_plantuml_figures.mjs
 */
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.join(__dirname, "..");
const outDir = path.join(root, "thesis_assets", "plantuml", "out");
const assetRoot = path.join(root, "thesis_assets");

const pairs = [
  ["fig3_1_business_flow"],
  ["fig3_2_use_case"],
  ["fig3_3_activity_search"],
  ["fig4_1_architecture"],
  ["fig4_2_modules"],
  ["fig4_3_sequence_async_summary", "fig_optional_sequence"],
  ["fig4_4_er", "fig_optional_er"],
];

if (!fs.existsSync(outDir)) {
  console.error("Missing:", outDir, "— run thesis_assets/plantuml/generate.bat first.");
  process.exit(1);
}

for (const row of pairs) {
  const [base, alias] = row;
  for (const ext of [".svg", ".png"]) {
    const src = path.join(outDir, base + ext);
    if (!fs.existsSync(src)) {
      console.warn("Skip (missing):", src);
      continue;
    }
    const dest1 = path.join(assetRoot, base + ext);
    fs.copyFileSync(src, dest1);
    console.log("→", path.relative(root, dest1));
    if (alias) {
      const dest2 = path.join(assetRoot, alias + ext);
      fs.copyFileSync(src, dest2);
      console.log("→", path.relative(root, dest2), "(alias)");
    }
  }
}
console.log("Done.");
