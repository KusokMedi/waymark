import countries from "i18n-iso-countries";
import enLocale from "i18n-iso-countries/langs/en.json";
import type { AdvisoryRecord, Destination, RecordKind } from "./model";
import { RAW_CODE_MAP, isKnownRawCode } from "./model";
import type { ArcGisFeature } from "./source";

countries.registerLocale(enLocale);

function asString(v: unknown): string | null {
  if (v === null || v === undefined) return null;
  const s = String(v).trim();
  if (s === "" || s === " ") return null;
  return s;
}

function asNumber(v: unknown): number | null {
  if (v === null || v === undefined) return null;
  const n = Number(v);
  return Number.isFinite(n) ? n : null;
}

function msToDate(v: unknown): string | null {
  if (v === null || v === undefined) return null;
  const n = Number(v);
  if (!Number.isFinite(n)) return null;
  // ArcGIS date fields are Unix milliseconds (UTC).
  const d = new Date(n);
  if (Number.isNaN(d.getTime())) return null;
  const y = d.getUTCFullYear();
  const m = String(d.getUTCMonth() + 1).padStart(2, "0");
  const day = String(d.getUTCDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

function msToIso(v: unknown): string | null {
  if (v === null || v === undefined) return null;
  const n = Number(v);
  if (!Number.isFinite(n)) return null;
  const d = new Date(n);
  if (Number.isNaN(d.getTime())) return null;
  return d.toISOString();
}

function validateUrl(v: unknown): string | null {
  const s = asString(v);
  if (!s) return null;
  try {
    const u = new URL(s);
    if (u.protocol !== "https:" && u.protocol !== "http:") return null;
    return u.toString();
  } catch {
    return null;
  }
}

export function normalizeFeature(feature: ArcGisFeature, index: number): AdvisoryRecord | null {
  const a = feature?.attributes;
  if (!a || typeof a !== "object") return null;

  const name = asString(a["NAME"]);
  if (!name) return null; // skip records without a destination name

  const iso3Raw = asString(a["ISO_3"]);
  const iso3 = iso3Raw ? iso3Raw.toUpperCase() : null;
  const iso2 = iso3 && /^[A-Z]{3}$/.test(iso3) ? (countries.alpha3ToAlpha2(iso3) ?? null) : null;

  const rawCode = asNumber(a["LEVEL_"]);
  let level: AdvisoryRecord["level"] = null;
  let higherRisk = false;
  let officialLabel: string | null = null;
  if (rawCode !== null && isKnownRawCode(rawCode)) {
    const mapped = RAW_CODE_MAP[rawCode];
    level = mapped.level;
    higherRisk = mapped.higherRisk;
    officialLabel = mapped.label;
  } else if (rawCode !== null) {
    officialLabel = `Unsupported advisory code: ${rawCode}`;
  }

  const advisoryUrl = validateUrl(a["URL"]);
  const advisoryDate = msToDate(a["ADVDATE"]);
  const sourceRecordId = a["GlobalID"] ? String(a["GlobalID"]) : (a["OBJECTID"] != null ? String(a["OBJECTID"]) : null);
  const sourceEditDate = msToIso(a["EditDate"]);

  const adStatus = asNumber(a["AD_OD_Status"]);
  const adText = asString(a["AD_OD_Text"]);
  const adDate = asString(a["AD_OD_Date"]); // already date-only string

  return {
    id: sourceRecordId ?? `record-${index}`,
    destinationName: name,
    isoAlpha3: iso3,
    isoAlpha2: iso2,
    rawLevelCode: rawCode,
    level,
    containsHigherRiskAreas: higherRisk,
    officialLabel,
    advisoryUrl,
    advisoryDate,
    sourceName: "U.S. Department of State (via ArcGIS Travel Advisory Levels)",
    sourceRecordId,
    recordKind: "unknown", // assigned during grouping
    assistedDepartureStatus: adStatus,
    assistedDepartureText: adText,
    assistedDepartureDate: adDate,
    sourceEditDate,
  };
}

export function normalizeFeatures(features: unknown[]): AdvisoryRecord[] {
  const out: AdvisoryRecord[] = [];
  if (!Array.isArray(features)) return out;
  features.forEach((f, i) => {
    const rec = normalizeFeature(f as ArcGisFeature, i);
    if (rec) out.push(rec);
  });
  return out;
}

export function classifyRecordKinds(records: AdvisoryRecord[]): AdvisoryRecord[] {
  // Group by ISO to decide country vs region.
  const byIso = new Map<string, AdvisoryRecord[]>();
  for (const r of records) {
    const key = r.isoAlpha3 ?? `name:${r.destinationName.toLowerCase()}`;
    if (!byIso.has(key)) byIso.set(key, []);
    byIso.get(key)!.push(r);
  }
  const result: AdvisoryRecord[] = [];
  for (const group of byIso.values()) {
    const iso = group[0].isoAlpha3;
    const officialCountryName = iso ? countries.getName(iso, "en") : null;
    for (const r of group) {
      let kind: RecordKind = "country";
      if (group.length > 1) {
        if (officialCountryName && r.destinationName.toLowerCase() === officialCountryName.toLowerCase()) {
          kind = "country";
        } else {
          // If there is a country-level record in the group, others are regions.
          const hasCountry = group.some(
            (x) => officialCountryName && x.destinationName.toLowerCase() === officialCountryName.toLowerCase()
          );
          kind = hasCountry ? "region" : "region";
        }
      }
      result.push({ ...r, recordKind: kind });
    }
  }
  return result;
}

export function groupIntoDestinations(records: AdvisoryRecord[]): Destination[] {
  const classified = classifyRecordKinds(records);
  const byIso = new Map<string, AdvisoryRecord[]>();
  for (const r of classified) {
    const key = r.isoAlpha3 ?? `name:${r.destinationName.toLowerCase()}`;
    if (!byIso.has(key)) byIso.set(key, []);
    byIso.get(key)!.push(r);
  }
  const destinations: Destination[] = [];
  for (const group of byIso.values()) {
    const countryRec = group.find((r) => r.recordKind === "country");
    const iso = group[0].isoAlpha3;
    const officialName = iso ? countries.getName(iso, "en") : null;
    const primaryName =
      countryRec?.destinationName ?? officialName ?? group[0].destinationName;

    // Aggregate level
    let level: Destination["level"] = null;
    if (countryRec && countryRec.level !== null) {
      level = countryRec.level;
    } else {
      const levels = group.map((r) => r.level).filter((l): l is 1 | 2 | 3 | 4 => l !== null);
      const unique = new Set(levels);
      if (levels.length > 0 && unique.size === 1) {
        level = levels[0];
      }
    }

    const containsHigherRiskAreas = group.some((r) => r.containsHigherRiskAreas);
    const advisoryUrl =
      countryRec?.advisoryUrl ?? group.find((r) => r.advisoryUrl)?.advisoryUrl ?? null;
    const advisoryDate =
      countryRec?.advisoryDate ?? group.find((r) => r.advisoryDate)?.advisoryDate ?? null;
    const adRec = countryRec ?? group.find((r) => r.assistedDepartureText || r.assistedDepartureStatus !== null);
    const assistedDepartureStatus = adRec?.assistedDepartureStatus ?? null;
    const assistedDepartureText = adRec?.assistedDepartureText ?? null;
    const assistedDepartureDate = adRec?.assistedDepartureDate ?? null;

    const iso2 = group[0].isoAlpha2 ?? (iso ? countries.alpha3ToAlpha2(iso) ?? null : null);

    destinations.push({
      id: iso ?? `name:${primaryName.toLowerCase()}`,
      isoAlpha3: iso,
      isoAlpha2: iso2,
      primaryName,
      records: group.slice().sort((a, b) => a.destinationName.localeCompare(b.destinationName)),
      level,
      containsHigherRiskAreas,
      officialLabel: countryRec?.officialLabel ?? (level !== null ? null : (group.find((r) => r.officialLabel)?.officialLabel ?? null)),
      advisoryUrl,
      advisoryDate,
      assistedDepartureStatus,
      assistedDepartureText,
      assistedDepartureDate,
    });
  }
  return destinations.sort((a, b) => a.primaryName.localeCompare(b.primaryName));
}
