import { useParams, Link } from "react-router-dom";
import { useApp } from "../App";
import { Flag } from "../components/Flag";
import { LevelChip } from "../components/LevelChip";
import { ExternalLink, Heart } from "lucide-react";
import type { AdvisoryRecord } from "../data/model";

export default function DestinationDetail() {
  const { id } = useParams<{ id: string }>();
  const { destinations, data, favorites, toggleFavorite } = useApp();
  const dest = destinations.find((d) => d.id === decodeURIComponent(id ?? ""));

  if (!dest) {
    return (
      <div className="empty">
        <p>Destination not found.</p>
        <Link to="/destinations" className="btn secondary">
          Back to all destinations
        </Link>
      </div>
    );
  }

  const favorite = favorites.has(dest.id);

  return (
    <div>
      <Link to="/destinations" className="btn secondary" style={{ marginBottom: "var(--space-4)", display: "inline-block", textDecoration: "none" }}>
        ← Back
      </Link>
      <div className="card" style={{ marginBottom: "var(--space-4)" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: "var(--space-3)", flexWrap: "wrap" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "var(--space-3)" }}>
            <Flag isoAlpha2={dest.isoAlpha2} name={dest.primaryName} />
            <div>
              <h1 style={{ margin: 0 }}>{dest.primaryName}</h1>
              <div className="freshness">
                {dest.isoAlpha3 ? `${dest.isoAlpha3}${dest.isoAlpha2 ? " / " + dest.isoAlpha2 : ""}` : "No ISO code"}
              </div>
            </div>
          </div>
          <button className="icon-btn" onClick={() => toggleFavorite(dest.id)} aria-label="Toggle favorite">
            <Heart size={18} fill={favorite ? "currentColor" : "none"} />
          </button>
        </div>

        <div style={{ marginTop: "var(--space-4)" }}>
          <LevelChip level={dest.level} higherRisk={dest.containsHigherRiskAreas} label={dest.officialLabel} />
        </div>

        {dest.level === null && dest.records.length > 1 && (
          <p style={{ marginTop: "var(--space-3)", color: "var(--on-surface-variant)" }}>
            This destination has regional advisories with differing levels. See the regional records below.
          </p>
        )}

        <div className="details-grid" style={{ marginTop: "var(--space-4)" }}>
          <div className="kv">
            <span className="k">Advisory date</span>
            <span className="v">{dest.advisoryDate ?? "Not provided"}</span>
          </div>
          <div className="kv">
            <span className="k">Higher-risk areas</span>
            <span className="v">{dest.containsHigherRiskAreas ? "Yes, contains areas with higher security risk" : "No special regional-risk indicator"}</span>
          </div>
          {dest.assistedDepartureStatus !== null && (
            <div className="kv">
              <span className="k">Assisted/ordered departure</span>
              <span className="v">
                {dest.assistedDepartureStatus === 1 ? "Authorized departure" : dest.assistedDepartureStatus === 2 ? "Ordered departure" : "Status recorded"}
                {dest.assistedDepartureText ? `, ${dest.assistedDepartureText}` : ""}
                {dest.assistedDepartureDate ? ` (${dest.assistedDepartureDate})` : ""}
              </span>
            </div>
          )}
          <div className="kv">
            <span className="k">Original advisory</span>
            <span className="v">
              {dest.advisoryUrl ? (
                <a href={dest.advisoryUrl} target="_blank" rel="noopener noreferrer">
                  Open official advisory <ExternalLink size={14} style={{ verticalAlign: "middle" }} />
                </a>
              ) : (
                "No link provided"
              )}
            </span>
          </div>
        </div>
      </div>

      {dest.records.length > 1 && (
        <div className="card" style={{ marginBottom: "var(--space-4)" }}>
          <h2 style={{ marginTop: 0 }}>Regional records</h2>
          <p style={{ color: "var(--on-surface-variant)", fontSize: "0.85rem" }}>
            Source records associated with this destination. Regional records are preserved as published.
          </p>
          <div style={{ display: "flex", flexDirection: "column", gap: "var(--space-3)" }}>
            {dest.records.map((r: AdvisoryRecord) => (
              <div key={r.id} style={{ padding: "var(--space-3)", background: "var(--surface-2)", borderRadius: "var(--radius-md)" }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "var(--space-2)" }}>
                  <strong>{r.destinationName}</strong>
                  <LevelChip level={r.level} higherRisk={r.containsHigherRiskAreas} label={r.officialLabel} />
                </div>
                <div className="freshness" style={{ marginTop: 4 }}>
                  {r.recordKind} · {r.advisoryDate ?? "no date"}
                  {r.assistedDepartureText ? ` · ${r.assistedDepartureText}` : ""}
                </div>
                {r.advisoryUrl && (
                  <a href={r.advisoryUrl} target="_blank" rel="noopener noreferrer" style={{ fontSize: "0.85rem" }}>
                    Official link
                  </a>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="card">
        <h2 style={{ marginTop: 0 }}>Data provenance</h2>
        <div className="kv">
          <span className="k">Source</span>
          <span className="v">{dest.records[0]?.sourceName ?? "U.S. Department of State"}</span>
        </div>
        <div className="kv">
          <span className="k">Snapshot retrieved</span>
          <span className="v">{data?.snapshot?.fetchedAt ?? "Unknown"}</span>
        </div>
        <div className="kv">
          <span className="k">Stale</span>
          <span className="v">{data?.stale ? "Yes, using cached data" : "No"}</span>
        </div>
        <p style={{ fontSize: "0.8rem", color: "var(--on-surface-variant)", marginTop: "var(--space-3)" }}>
          Advisories are published by the U.S. Department of State for U.S. citizens, nationals, and legal residents. They are not a universal safety assessment for all travelers.
        </p>
      </div>
    </div>
  );
}
