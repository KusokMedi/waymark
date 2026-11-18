import { createContext, useContext, useEffect, useState } from "react";
import { Routes, Route, NavLink } from "react-router-dom";
import { loadSnapshot, refreshFromSource, type LoadedData } from "./data/loader";
import {
  loadFavorites,
  saveFavorites,
  loadTheme,
  saveTheme,
  loadView,
  saveView,
  loadWatchlist,
  saveWatchlist,
  type ThemePreference,
  type ViewMode,
} from "./lib/storage";
import { Sun, Moon, Monitor, RefreshCw } from "lucide-react";
import Dashboard from "./views/Dashboard";
import Destinations from "./views/Destinations";
import DestinationDetail from "./views/DestinationDetail";
import About from "./views/About";

interface AppContextValue {
  data: LoadedData | null;
  loading: boolean;
  refresh: () => Promise<void>;
  favorites: Set<string>;
  toggleFavorite: (id: string) => void;
  watchlist: string[];
  setWatchlist: (ids: string[]) => void;
  view: ViewMode;
  setView: (v: ViewMode) => void;
  theme: ThemePreference;
  setTheme: (t: ThemePreference) => void;
  destinations: LoadedData["destinations"];
}

const AppContext = createContext<AppContextValue | null>(null);
export function useApp(): AppContextValue {
  const ctx = useContext(AppContext);
  if (!ctx) throw new Error("useApp must be used within AppProvider");
  return ctx;
}

export default function App() {
  const [data, setData] = useState<LoadedData | null>(null);
  const [loading, setLoading] = useState(true);
  const [favorites, setFavorites] = useState<string[]>(loadFavorites());
  const [watchlist, setWatchlistState] = useState<string[]>(loadWatchlist());
  const [view, setViewState] = useState<ViewMode>(loadView());
  const [theme, setThemeState] = useState<ThemePreference>(loadTheme());

  useEffect(() => {
    let cancelled = false;
    loadSnapshot().then((d) => {
      if (!cancelled) {
        setData(d);
        setLoading(false);
      }
    });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    saveFavorites(favorites);
  }, [favorites]);
  useEffect(() => {
    saveView(view);
  }, [view]);
  useEffect(() => {
    saveWatchlist(watchlist);
  }, [watchlist]);
  useEffect(() => {
    saveTheme(theme);
    applyTheme(theme);
  }, [theme]);

  function applyTheme(t: ThemePreference) {
    const dark =
      t === "dark" ||
      (t === "system" && window.matchMedia("(prefers-color-scheme: dark)").matches);
    document.documentElement.setAttribute("data-theme", dark ? "dark" : "light");
    document.documentElement.setAttribute("data-theme-pref", t);
  }

  async function refresh() {
    setLoading(true);
    const d = await refreshFromSource();
    setData(d);
    setLoading(false);
  }

  const toggleFavorite = (id: string) => {
    setFavorites((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]));
  };

  const setWatchlist = (ids: string[]) => setWatchlistState(ids);
  const setView = (v: ViewMode) => setViewState(v);
  const setTheme = (t: ThemePreference) => setThemeState(t);

  const value: AppContextValue = {
    data,
    loading,
    refresh,
    favorites: new Set(favorites),
    toggleFavorite,
    watchlist,
    setWatchlist,
    view,
    setView,
    theme,
    setTheme,
    destinations: data?.destinations ?? [],
  };

  return (
    <AppContext.Provider value={value}>
      <div className="app-shell">
        <header className="topbar">
          <span className="brand">Waymark</span>
          <nav className="nav">
            <NavLink to="/" end className={({ isActive }) => (isActive ? "active" : "")}>
              Dashboard
            </NavLink>
            <NavLink to="/destinations" className={({ isActive }) => (isActive ? "active" : "")}>
              All destinations
            </NavLink>
            <NavLink to="/about" className={({ isActive }) => (isActive ? "active" : "")}>
              About
            </NavLink>
          </nav>
          <div className="spacer" />
          <button className="icon-btn" onClick={refresh} title="Refresh data" aria-label="Refresh data">
            <RefreshCw size={16} />
          </button>
          <ThemeToggle theme={theme} setTheme={setTheme} />
        </header>
        <main className="content">
          {loading && !data ? (
            <div className="empty">
              <div className="spinner" />
              <p>Loading advisories…</p>
            </div>
          ) : (
            <Routes>
              <Route path="/" element={<Dashboard />} />
              <Route path="/destinations" element={<Destinations />} />
              <Route path="/destinations/:id" element={<DestinationDetail />} />
              <Route path="/about" element={<About />} />
            </Routes>
          )}
          {data?.error && (
            <div className="card" style={{ marginTop: 12, borderColor: "var(--level-3)" }}>
              <strong>Data notice:</strong> {data.error}
              {data.stale && " (showing cached data)"}
            </div>
          )}
        </main>
        <footer style={{ padding: "var(--space-4)", textAlign: "center", color: "var(--on-surface-variant)", fontSize: "0.8rem" }}>
          Waymark is an independent open-source project. Data: U.S. Department of State. Not an official product.
        </footer>
      </div>
    </AppContext.Provider>
  );
}

function ThemeToggle({ theme, setTheme }: { theme: ThemePreference; setTheme: (t: ThemePreference) => void }) {
  const cycle = () => {
    const order: ThemePreference[] = ["system", "light", "dark"];
    const idx = order.indexOf(theme);
    setTheme(order[(idx + 1) % order.length]);
  };
  const Icon = theme === "dark" ? Moon : theme === "light" ? Sun : Monitor;
  return (
    <button className="icon-btn" onClick={cycle} aria-label="Toggle theme" title={`Theme: ${theme}`}>
      <Icon size={16} />
    </button>
  );
}
