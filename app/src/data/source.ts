
export const ARCGIS_SERVICE_URL =
  "https://services6.arcgis.com/R6wlO6UHmSzqm9Vs/ArcGIS/rest/services/Travel_Advisory_Levels__%282024%29_View_Layer/FeatureServer";
export const ARCGIS_LAYER_ID = 54;

export interface ArcGisFeature {
  attributes?: Record<string, unknown> | null;
}

export interface ArcGisQueryResponse {
  features?: ArcGisFeature[];
  exceededTransferLimit?: boolean;
  error?: { code?: number; message?: string };
}

export function buildQueryUrl(offset: number, pageSize: number): string {
  const params = new URLSearchParams({
    where: "NAME IS NOT NULL",
    outFields: "*",
    returnGeometry: "false",
    resultOffset: String(offset),
    resultRecordCount: String(pageSize),
    orderByFields: "OBJECTID",
    f: "json",
  });
  return `${ARCGIS_SERVICE_URL}/${ARCGIS_LAYER_ID}/query?${params.toString()}`;
}

export async function fetchAllFeatures(
  fetchImpl: typeof fetch = fetch,
  pageSize = 2000
): Promise<ArcGisFeature[]> {
  const collected: ArcGisFeature[] = [];
  let offset = 0;
  // Safety bound
  for (let i = 0; i < 50; i++) {
    const res = await fetchImpl(buildQueryUrl(offset, pageSize), {
      method: "GET",
      headers: { Accept: "application/json" },
    });
    if (!res.ok) {
      throw new Error(`Source request failed: ${res.status} ${res.statusText}`);
    }
    const json = (await res.json()) as ArcGisQueryResponse;
    if (json.error) {
      throw new Error(`Source error: ${json.error.message ?? json.error.code}`);
    }
    const features = Array.isArray(json.features) ? json.features : [];
    collected.push(...features);
    if (!json.exceededTransferLimit || features.length === 0) {
      break;
    }
    offset += features.length;
  }
  return collected;
}