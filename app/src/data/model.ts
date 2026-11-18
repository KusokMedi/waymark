export type AdvisoryLevel = 1 | 2 | 3 | 4;

export type RecordKind = "country" | "region" | "unknown";

export interface AdvisoryRecord {
  id: string;
  destinationName: string;
  isoAlpha3: string | null;
  isoAlpha2: string | null;
  rawLevelCode: number | null;
  level: AdvisoryLevel | null;
  containsHigherRiskAreas: boolean;
  officialLabel: string | null;
  advisoryUrl: string | null;
  advisoryDate: string | null; // ISO date YYYY-MM-DD (date only)
  sourceName: string;
  sourceRecordId: string | null;
  recordKind: RecordKind;
  assistedDepartureStatus: number | null;
  assistedDepartureText: string | null;
  assistedDepartureDate: string | null;
  sourceEditDate: string | null; // ISO datetime of source record edit
}

export interface Destination {
  // A destination groups one or more source records that share an ISO code.
  id: string; // ISO alpha-3 or fallback
  isoAlpha3: string | null;
  isoAlpha2: string | null;
  primaryName: string;
  records: AdvisoryRecord[];
  // Aggregate display level: from the country-level record if present, else null.
  level: AdvisoryLevel | null;
  containsHigherRiskAreas: boolean;
  officialLabel: string | null;
  advisoryUrl: string | null;
  advisoryDate: string | null;
  assistedDepartureStatus: number | null;
  assistedDepartureText: string | null;
  assistedDepartureDate: string | null;
}

export interface DataSnapshot {
  schemaVersion: number;
  sourceName: string;
  sourceUrl: string;
  fetchedAt: string; // ISO datetime when Waymark fetched
  sourceLastEditDate: string | null;
  recordCount?: number;
  features?: unknown[];
  records: AdvisoryRecord[];
  destinations: Destination[];
}

export const SNAPSHOT_SCHEMA_VERSION = 1;

export const LEVEL_LABELS: Record<AdvisoryLevel, string> = {
  1: "Exercise Normal Precautions",
  2: "Exercise Increased Caution",
  3: "Reconsider Travel",
  4: "Do Not Travel",
};

export const LEVEL_DESCRIPTIONS: Record<AdvisoryLevel, string> = {
  1: "Exercise normal precautions.",
  2: "Exercise increased caution.",
  3: "Reconsider travel.",
  4: "Do not travel.",
};

// Explicit raw-code mapping. Never derive by arithmetic.
export const RAW_CODE_MAP: Record<number, { level: AdvisoryLevel; higherRisk: boolean; label: string }> = {
  1: { level: 1, higherRisk: false, label: "Exercise Normal Precautions" },
  10: { level: 1, higherRisk: true, label: "Exercise Normal Precautions - Contains Areas with Higher Security Risk" },
  2: { level: 2, higherRisk: false, label: "Exercise Increased Caution" },
  20: { level: 2, higherRisk: true, label: "Exercise Increased Caution - Contains Areas with Higher Security Risk" },
  3: { level: 3, higherRisk: false, label: "Reconsider Travel" },
  30: { level: 3, higherRisk: true, label: "Reconsider Travel - Contains Areas with Higher Security Risk" },
  4: { level: 4, higherRisk: false, label: "Do Not Travel" },
};

export function isKnownRawCode(code: number | null): code is number {
  return code !== null && Object.prototype.hasOwnProperty.call(RAW_CODE_MAP, code);
}

export const SEVERITY_ORDER: Record<AdvisoryLevel, number> = { 4: 0, 3: 1, 2: 2, 1: 3 };

export function compareSeverity(a: Destination, b: Destination): number {
  // unknown levels sort last
  const la = a.level;
  const lb = b.level;
  if (la === null && lb === null) return 0;
  if (la === null) return 1;
  if (lb === null) return -1;
  if (SEVERITY_ORDER[la] !== SEVERITY_ORDER[lb]) {
    return SEVERITY_ORDER[la] - SEVERITY_ORDER[lb];
  }
  // tie-break: higher-risk first
  if (a.containsHigherRiskAreas !== b.containsHigherRiskAreas) {
    return a.containsHigherRiskAreas ? -1 : 1;
  }
  return a.primaryName.localeCompare(b.primaryName);
}