import fs from "node:fs/promises";
import path from "node:path";

const root = process.cwd();
const dist = path.join(root, "dist");
const websiteDist = path.join(root, "website", "dist");

async function copyDir(src, dest) {
  await fs.mkdir(dest, { recursive: true });
  const entries = await fs.readdir(src, { withFileTypes: true });
  for (const e of entries) {
    const s = path.join(src, e.name);
    const d = path.join(dest, e.name);
    if (e.isDirectory()) await copyDir(s, d);
    else await fs.copyFile(s, d);
  }
}

async function main() {
  // Only the product website is published. The legacy web dashboard in app/
  // is not part of the GitHub Pages deployment.
  await fs.rm(dist, { recursive: true, force: true });
  await fs.mkdir(dist, { recursive: true });
  await copyDir(websiteDist, dist);
  console.log("Assembled dist/ from website/");
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
