import type { DataSnapshot, AdvisoryRecord } from "./model";
import { groupIntoDestinations, normalizeFeatures } from "./normalize";
import { fetchAllFeatures } from "./source";

const SNAPSHOT_URL = "data/latest.json";
const CACHE_KEY = "waymark:data:cache:v1";
const SNAPSHOT_SCHEMA_VERSION = 1;

export interface LoadedData {
  snapshot: DataSnapshot;
  destinations: ReturnType<typeof groupIntoDestinations>;
  records: AdvisoryRecord[];
  fromCache: boolean;
  stale: boolean;
  error: string | null;
}

interface CacheEntry {
  fetchedAt: string;
  snapshot: DataSnapshot;
}

function readCache(): CacheEntry | null {
  try {
    const raw = localStorage.getItem(CACHE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as CacheEntry;
    if (!parsed || typeof parsed !== "object") return null;
    if (!parsed.snapshot || parsed.snapshot.schemaVersion !== SNAPSHOT_SCHEMA_VERSION) return null;
    return parsed;
  } catch {
    return null;
  }
}

function writeCache(snapshot: DataSnapshot): void {
  try {
    const entry: CacheEntry = { fetchedAt: snapshot.fetchedAt, snapshot };
    localStorage.setItem(CACHE_KEY, JSON.stringify(entry));
  } catch {
    // ignore storage errors
  }
}

function validateSnapshot(raw: unknown): DataSnapshot | null {
  if (!raw || typeof raw !== "object") return null;
  const r = raw as Record<string, unknown>;
  if (r.schemaVersion !== SNAPSHOT_SCHEMA_VERSION) return null;
  if (typeof r.fetchedAt !== "string") return null;
  if (!Array.isArray(r.features)) return null;
  return r as unknown as DataSnapshot;
}

function buildSnapshot(features: unknown[], fetchedAt: string, sourceLastEdit: string | null): DataSnapshot {
  return {
    schemaVersion: SNAPSHOT_SCHEMA_VERSION,
    sourceName: "U.S. Department of State Travel Advisories (ArcGIS)",
    sourceUrl: "https://travel.state.gov/en/international-travel/travel-advisories.html",
    fetchedAt,
    sourceLastEditDate: sourceLastEdit,
    records: normalizeFeatures(features) as AdvisoryRecord[],
    destinations: groupIntoDestinations(normalizeFeatures(features)),
  };
}

// Normalize snapshot records/destinations if missing (e.g. from sync file features only).
function ensureNormalized(snapshot: DataSnapshot): DataSnapshot {
  if (Array.isArray(snapshot.records) && Array.isArray(snapshot.destinations) && snapshot.destinations.length > 0) {
    return snapshot;
  }
  const records = normalizeFeatures(snapshot.features ?? []);
  return {
    ...snapshot,
    records,
    destinations: groupIntoDestinations(records),
  };
}

export async function loadSnapshot(): Promise<LoadedData> {
  // Try network (same-origin snapshot) first.
  let networkError: string | null = null;
  try {
    const res = await fetch(SNAPSHOT_URL, { cache: "no-cache" });
    if (res.ok) {
      const raw = await res.json();
      const snap = validateSnapshot(raw);
      if (snap) {
        const normalized = ensureNormalized(snap);
        writeCache(normalized);
        return {
          snapshot: normalized,
          destinations: normalized.destinations,
          records: normalized.records,
          fromCache: false,
          stale: false,
          error: null,
        };
      }
      networkError = "Invalid snapshot format";
    } else {
      networkError = `HTTP ${res.status}`;
    }
  } catch (e) {
    networkError = e instanceof Error ? e.message : "Network error";
  }

  // Fall back to cache.
  const cache = readCache();
  if (cache) {
    const normalized = ensureNormalized(cache.snapshot);
    return {
      snapshot: normalized,
      destinations: normalized.destinations,
      records: normalized.records,
      fromCache: true,
      stale: true,
      error: networkError,
    };
  }

  // Last resort: try direct source fetch (CORS-permitting).
  try {
    const features = await fetchAllFeatures();
    const snap = buildSnapshot(features, new Date().toISOString(), null);
    writeCache(snap);
    return {
      snapshot: snap,
      destinations: snap.destinations,
      records: snap.records,
      fromCache: false,
      stale: false,
      error: null,
    };
  } catch (e) {
    return {
      snapshot: null as unknown as DataSnapshot,
      destinations: [],
      records: [],
      fromCache: false,
      stale: true,
      error: e instanceof Error ? e.message : "Failed to load data",
    };
  }
}

export async function refreshFromSource(): Promise<LoadedData> {
  try {
    const features = await fetchAllFeatures();
    const snap = buildSnapshot(features, new Date().toISOString(), null);
    writeCache(snap);
    return {
      snapshot: snap,
      destinations: snap.destinations,
      records: snap.records,
      fromCache: false,
      stale: false,
      error: null,
    };
  } catch (e) {
    // Fall back to snapshot
    return loadSnapshot();
  }
}
