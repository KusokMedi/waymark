import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useApp } from "../App";
import { Flag } from "../components/Flag";
import { LevelChip } from "../components/LevelChip";
import { Search, Heart, List, LayoutGrid } from "lucide-react";
import type { Destination, AdvisoryLevel } from "../data/model";
import { compareSeverity } from "../data/model";

type SortMode = "name" | "severity";

export default function Destinations() {
  const { destinations, view, setView, favorites, toggleFavorite } = useApp();
  const [query, setQuery] = useState("");
  const [sort, setSort] = useState<SortMode>("name");
  const [levelFilter, setLevelFilter] = useState<Set<AdvisoryLevel | "unknown" | "higher">>(new Set());
  const [favoritesOnly, setFavoritesOnly] = useState(false);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    let list = destinations;
    if (q) {
      list = list.filter((d) => d.primaryName.toLowerCase().includes(q));
    }
    if (favoritesOnly) {
      list = list.filter((d) => favorites.has(d.id));
    }
    if (levelFilter.size > 0) {
      list = list.filter((d) => {
        const key = d.level === null ? "unknown" : d.level;
        if (levelFilter.has("higher") && d.containsHigherRiskAreas) return true;
        return levelFilter.has(key as AdvisoryLevel | "unknown");
      });
    }
    const sorted = [...list];
    if (sort === "name") {
      sorted.sort((a, b) => a.primaryName.localeCompare(b.primaryName));
    } else {
      sorted.sort(compareSeverity);
    }
    return sorted;
  }, [destinations, query, sort, levelFilter, favoritesOnly, favorites]);

  const toggleLevel = (k: AdvisoryLevel | "unknown" | "higher") => {
    setLevelFilter((prev) => {
      const next = new Set(prev);
      if (next.has(k)) next.delete(k);
      else next.add(k);
      return next;
    });
  };

  const clearFilters = () => {
    setLevelFilter(new Set());
    setFavoritesOnly(false);
    setQuery("");
  };

  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "var(--space-4)", flexWrap: "wrap", gap: "var(--space-3)" }}>
        <h1 style={{ margin: 0 }}>All destinations ({destinations.length})</h1>
        <div style={{ display: "flex", gap: "var(--space-2)" }}>
          <button className={`icon-btn ${view === "list" ? "active" : ""}`} onClick={() => setView("list")} aria-label="List view">
            <List size={16} />
          </button>
          <button className={`icon-btn ${view === "card" ? "active" : ""}`} onClick={() => setView("card")} aria-label="Card view">
            <LayoutGrid size={16} />
          </button>
        </div>
      </div>

      <div className="card" style={{ marginBottom: "var(--space-4)", display: "flex", flexWrap: "wrap", gap: "var(--space-3)", alignItems: "center" }}>
        <div style={{ flex: "1 1 200px", position: "relative" }}>
          <Search size={16} style={{ position: "absolute", left: 10, top: 10, color: "var(--on-surface-variant)" }} />
          <input
            className="input"
            style={{ paddingLeft: 34 }}
            placeholder="Search destinations…"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            aria-label="Search destinations"
          />
        </div>
        <select className="select" value={sort} onChange={(e) => setSort(e.target.value as SortMode)} aria-label="Sort">
          <option value="name">Sort: Name</option>
          <option value="severity">Sort: Severity</option>
        </select>
        <button className={`icon-btn ${favoritesOnly ? "active" : ""}`} onClick={() => setFavoritesOnly((v) => !v)} title="Favorites only">
          <Heart size={16} />
        </button>
      </div>

      <div style={{ display: "flex", flexWrap: "wrap", gap: "var(--space-2)", marginBottom: "var(--space-4)" }}>
        {([1, 2, 3, 4] as AdvisoryLevel[]).map((l) => (
          <button
            key={l}
            className={`icon-btn ${levelFilter.has(l) ? "active" : ""}`}
            onClick={() => toggleLevel(l)}
          >
            Level {l}
          </button>
        ))}
        <button className={`icon-btn ${levelFilter.has("unknown") ? "active" : ""}`} onClick={() => toggleLevel("unknown")}>
          Unknown
        </button>
        <button className={`icon-btn ${levelFilter.has("higher") ? "active" : ""}`} onClick={() => toggleLevel("higher")}>
          Higher-risk areas
        </button>
        <button className="icon-btn" onClick={clearFilters}>
          Clear
        </button>
      </div>

      {filtered.length === 0 ? (
        <div className="empty">
          <div className="empty-state-icon">🔍</div>
          <p>No destinations match your filters.</p>
        </div>
      ) : view === "list" ? (
        <div style={{ display: "flex", flexDirection: "column", gap: "var(--space-2)" }}>
          {filtered.map((d) => (
            <ListRow key={d.id} d={d} favorite={favorites.has(d.id)} onToggle={() => toggleFavorite(d.id)} />
          ))}
        </div>
      ) : (
        <div className="grid cards">
          {filtered.map((d) => (
            <Card key={d.id} d={d} favorite={favorites.has(d.id)} onToggle={() => toggleFavorite(d.id)} />
          ))}
        </div>
      )}
    </div>
  );
}

function ListRow({ d, favorite, onToggle }: { d: Destination; favorite: boolean; onToggle: () => void }) {
  return (
    <div className="list-row">
      <Flag isoAlpha2={d.isoAlpha2} name={d.primaryName} />
      <Link to={`/destinations/${encodeURIComponent(d.id)}`} style={{ textDecoration: "none", color: "var(--on-surface)", fontWeight: 500 }}>
        {d.primaryName}
      </Link>
      <LevelChip level={d.level} higherRisk={d.containsHigherRiskAreas} />
      <button className="icon-btn" onClick={onToggle} aria-label="Toggle favorite">
        <Heart size={14} fill={favorite ? "currentColor" : "none"} />
      </button>
    </div>
  );
}

function Card({ d, favorite, onToggle }: { d: Destination; favorite: boolean; onToggle: () => void }) {
  return (
    <div className="card" style={{ display: "flex", flexDirection: "column", gap: "var(--space-3)" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <div style={{ display: "flex", alignItems: "center", gap: "var(--space-2)" }}>
          <Flag isoAlpha2={d.isoAlpha2} name={d.primaryName} />
          <Link to={`/destinations/${encodeURIComponent(d.id)}`} style={{ fontWeight: 600, textDecoration: "none", color: "var(--on-surface)" }}>
            {d.primaryName}
          </Link>
        </div>
        <button className="icon-btn" onClick={onToggle} aria-label="Toggle favorite">
          <Heart size={16} fill={favorite ? "currentColor" : "none"} />
        </button>
      </div>
      <LevelChip level={d.level} higherRisk={d.containsHigherRiskAreas} />
      {d.advisoryDate && <span className="freshness">Advisory date: {d.advisoryDate}</span>}
    </div>
  );
}
