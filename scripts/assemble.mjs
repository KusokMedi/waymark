import fs from "node:fs/promises";
import path from "node:path";

const root = process.cwd();
const dist = path.join(root, "dist");
const websiteDist = path.join(root, "website", "dist");
const appDist = path.join(root, "app", "dist");

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

async function rmrf(p) {
  await fs.rm(p, { recursive: true, force: true });
}

async function main() {
  await rmrf(dist);
  await fs.mkdir(dist, { recursive: true });
  await copyDir(websiteDist, dist);
  await copyDir(appDist, path.join(dist, "app"));
  // Ensure data snapshot present
  const dataSrc = path.join(root, "data", "latest.json");
  const dataDest = path.join(dist, "app", "data", "latest.json");
  try {
    await fs.mkdir(path.dirname(dataDest), { recursive: true });
    await fs.copyFile(dataSrc, dataDest);
  } catch (e) {
    console.warn("No data snapshot to copy:", e.message);
  }
  console.log("Assembled dist/");
}

main().catch((e) => { console.error(e); process.exit(1); });
