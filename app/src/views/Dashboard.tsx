import { useState } from "react";
import { Link } from "react-router-dom";
import { Heart } from "lucide-react";
import { useApp } from "../App";
import { Flag } from "../components/Flag";
import { LevelChip } from "../components/LevelChip";
import type { Destination } from "../data/model";

export default function Dashboard() {
  const { destinations, watchlist, setWatchlist, favorites, toggleFavorite } = useApp();
  const [picker, setPicker] = useState("");

  const destMap = new Map(destinations.map((d) => [d.id, d]));
  const watched = watchlist.map((id) => destMap.get(id)).filter(Boolean) as Destination[];

  const add = () => {
    if (picker && !watchlist.includes(picker)) {
      setWatchlist([...watchlist, picker]);
    }
    setPicker("");
  };
  const remove = (id: string) => setWatchlist(watchlist.filter((x) => x !== id));

  const available = destinations.filter((d) => !watchlist.includes(d.id));

  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "var(--space-4)" }}>
        <h1 style={{ margin: 0 }}>Dashboard</h1>
        <div style={{ display: "flex", gap: "var(--space-2)", alignItems: "center" }}>
          <select className="select" value={picker} onChange={(e) => setPicker(e.target.value)} aria-label="Add destination">
            <option value="">Add destination…</option>
            {available.map((d) => (
              <option key={d.id} value={d.id}>
                {d.primaryName}
              </option>
            ))}
          </select>
          <button className="btn" onClick={add} disabled={!picker}>
            Add
          </button>
        </div>
      </div>

      {watched.length === 0 ? (
        <div className="empty">
          <div className="empty-state-icon">🌍</div>
          <p>No destinations in your watchlist yet. Add one above.</p>
        </div>
      ) : (
        <div className="grid cards">
          {watched.map((d) => (
            <DestinationCard
              key={d.id}
              d={d}
              favorite={favorites.has(d.id)}
              onToggleFavorite={() => toggleFavorite(d.id)}
              onRemove={() => remove(d.id)}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function DestinationCard({
  d,
  favorite,
  onToggleFavorite,
  onRemove,
}: {
  d: Destination;
  favorite: boolean;
  onToggleFavorite: () => void;
  onRemove: () => void;
}) {
  return (
    <div className="card" style={{ display: "flex", flexDirection: "column", gap: "var(--space-3)" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <div style={{ display: "flex", alignItems: "center", gap: "var(--space-2)" }}>
          <Flag isoAlpha2={d.isoAlpha2} name={d.primaryName} />
          <Link to={`/destinations/${encodeURIComponent(d.id)}`} style={{ fontWeight: 600, textDecoration: "none", color: "var(--on-surface)" }}>
            {d.primaryName}
          </Link>
        </div>
        <button className="icon-btn" onClick={onToggleFavorite} aria-label="Toggle favorite">
          <Heart size={16} fill={favorite ? "currentColor" : "none"} />
        </button>
      </div>
      <LevelChip level={d.level} higherRisk={d.containsHigherRiskAreas} />
      {d.level === null && d.records.length > 1 && (
        <span style={{ fontSize: "0.78rem", color: "var(--on-surface-variant)" }}>Varies by region</span>
      )}
      {d.advisoryDate && <span className="freshness">Advisory date: {d.advisoryDate}</span>}
      {d.assistedDepartureText && (
        <span style={{ fontSize: "0.78rem", color: "var(--level-3)" }}>{d.assistedDepartureText}</span>
      )}
      <div style={{ display: "flex", justifyContent: "space-between" }}>
        <Link to={`/destinations/${encodeURIComponent(d.id)}`} className="btn secondary" style={{ textDecoration: "none" }}>
          Details
        </Link>
        <button className="btn secondary" onClick={onRemove}>
          Remove
        </button>
      </div>
    </div>
  );
}
