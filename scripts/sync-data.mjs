import fs from "node:fs/promises";
import path from "node:path";

const SERVICE_URL =
  "https://services6.arcgis.com/R6wlO6UHmSzqm9Vs/ArcGIS/rest/services/Travel_Advisory_Levels__%282024%29_View_Layer/FeatureServer";
const LAYER_ID = 54;

function buildUrl(offset, pageSize) {
  const params = new URLSearchParams({
    where: "NAME IS NOT NULL",
    outFields: "*",
    returnGeometry: "false",
    resultOffset: String(offset),
    resultRecordCount: String(pageSize),
    orderByFields: "OBJECTID",
    f: "json",
  });
  return `${SERVICE_URL}/${LAYER_ID}/query?${params.toString()}`;
}

async function fetchAll() {
  const collected = [];
  let offset = 0;
  for (let i = 0; i < 50; i++) {
    const res = await fetch(buildUrl(offset, 2000));
    if (!res.ok) throw new Error(`HTTP ${res.status} ${res.statusText}`);
    const json = await res.json();
    if (json.error) throw new Error(json.error.message ?? `ArcGIS error ${json.error.code}`);
    const features = Array.isArray(json.features) ? json.features : [];
    collected.push(...features);
    if (!json.exceededTransferLimit || features.length === 0) break;
    offset += features.length;
  }
  if (collected.length === 0) throw new Error("No features returned");
  return collected;
}

async function main() {
  const features = await fetchAll();
  const snapshot = {
    schemaVersion: 1,
    sourceName: "U.S. Department of State Travel Advisories (ArcGIS)",
    sourceUrl: "https://travel.state.gov/en/international-travel/travel-advisories.html",
    dataServiceUrl: `${SERVICE_URL}/${LAYER_ID}`,
    fetchedAt: new Date().toISOString(),
    recordCount: features.length,
    features,
  };
  const root = path.resolve(process.cwd());
  const targets = [
    path.join(root, "data", "latest.json"),
    path.join(root, "app", "public", "data", "latest.json"),
  ];
  for (const t of targets) {
    await fs.mkdir(path.dirname(t), { recursive: true });
    await fs.writeFile(t, JSON.stringify(snapshot, null, 2));
  }
  console.log(`Wrote ${features.length} features to ${targets.join(", ")}`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
