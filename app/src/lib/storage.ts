const FAVORITES_KEY = "waymark:favorites:v1";
const THEME_KEY = "waymark:theme:v1";
const VIEW_KEY = "waymark:view:v1";
const WATCHLIST_KEY = "waymark:watchlist:v1";

export type ThemePreference = "system" | "light" | "dark";
export type ViewMode = "list" | "card";

export function loadFavorites(): string[] {
  try {
    const raw = localStorage.getItem(FAVORITES_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed.filter((x) => typeof x === "string");
  } catch {
    return [];
  }
}

export function saveFavorites(ids: string[]): void {
  try {
    localStorage.setItem(FAVORITES_KEY, JSON.stringify(ids));
  } catch {
    // ignore
  }
}

export function loadTheme(): ThemePreference {
  try {
    const raw = localStorage.getItem(THEME_KEY);
    if (raw === "light" || raw === "dark" || raw === "system") return raw;
    return "system";
  } catch {
    return "system";
  }
}

export function saveTheme(t: ThemePreference): void {
  try {
    localStorage.setItem(THEME_KEY, t);
  } catch {
    // ignore
  }
}

export function loadView(): ViewMode {
  try {
    const raw = localStorage.getItem(VIEW_KEY);
    if (raw === "list" || raw === "card") return raw;
    return "list";
  } catch {
    return "list";
  }
}

export function saveView(v: ViewMode): void {
  try {
    localStorage.setItem(VIEW_KEY, v);
  } catch {
    // ignore
  }
}

export function loadWatchlist(): string[] {
  // default Finland, Estonia, Latvia, Lithuania by ISO
  const defaults = ["FIN", "EST", "LVA", "LTU"];
  try {
    const raw = localStorage.getItem(WATCHLIST_KEY);
    if (!raw) return defaults;
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return defaults;
    const ids = parsed.filter((x) => typeof x === "string");
    return ids.length ? ids : defaults;
  } catch {
    return defaults;
  }
}

export function saveWatchlist(ids: string[]): void {
  try {
    localStorage.setItem(WATCHLIST_KEY, JSON.stringify(ids));
  } catch {
    // ignore
  }
}
