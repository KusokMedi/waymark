import { useMemo, useState } from "react";
import {
  ArrowRight,
  Bell,
  Check,
  ChevronRight,
  Database,
  Download,
  Github,
  Globe2,
  History,
  LayoutGrid,
  Lock,
  MapPin,
  RefreshCw,
  Shield,
  Smartphone,
  Sparkles,
  Star,
  WifiOff,
} from "lucide-react";

type Level = 1 | 2 | 3 | 4;

interface SampleCountry {
  id: string;
  name: string;
  iso: string;
  level: Level;
  higherRisk?: boolean;
  date: string;
  region?: string;
  favorite?: boolean;
}

const SAMPLE_COUNTRIES: SampleCountry[] = [
  { id: "FIN", name: "Finland", iso: "FI", level: 1, date: "13 Mar 2026", favorite: true },
  { id: "EST", name: "Estonia", iso: "EE", level: 1, date: "4 Feb 2026" },
  { id: "LVA", name: "Latvia", iso: "LV", level: 2, date: "19 Jan 2026" },
  { id: "LTU", name: "Lithuania", iso: "LT", level: 1, date: "2 Mar 2026" },
  { id: "MEX", name: "Mexico", iso: "MX", level: 2, higherRisk: true, date: "27 Feb 2026", region: "Jalisco — Reconsider Travel" },
  { id: "JPN", name: "Japan", iso: "JP", level: 1, date: "10 Mar 2026", favorite: true },
  { id: "COL", name: "Colombia", iso: "CO", level: 3, higherRisk: true, date: "6 Feb 2026", region: "Arauca — Do Not Travel" },
  { id: "EGY", name: "Egypt", iso: "EG", level: 2, higherRisk: true, date: "30 Jan 2026", region: "Sinai — Reconsider Travel" },
];

const LEVEL_LABELS: Record<Level, string> = {
  1: "Exercise Normal Precautions",
  2: "Exercise Increased Caution",
  3: "Reconsider Travel",
  4: "Do Not Travel",
};

const LEVEL_NOTES: Record<Level, string> = {
  1: "The lowest advisory level. Ordinary precautions still apply.",
  2: "Stay alert and avoid areas with elevated risk.",
  3: "Avoid travel unless essential. Review official guidance carefully.",
  4: "Do not travel. The highest advisory level.",
};

function levelClass(level: Level) {
  return `level level-${level}`;
}

export default function App() {
  const [selectedId, setSelectedId] = useState("FIN");
  const [tab, setTab] = useState<"watchlist" | "explore">("watchlist");
  const [query, setQuery] = useState("");
  const selected = SAMPLE_COUNTRIES.find((c) => c.id === selectedId) ?? SAMPLE_COUNTRIES[0];

  const visible = useMemo(() => {
    const base = tab === "watchlist" ? SAMPLE_COUNTRIES.slice(0, 5) : SAMPLE_COUNTRIES;
    return query.trim()
      ? base.filter((c) => c.name.toLowerCase().includes(query.trim().toLowerCase()))
      : base;
  }, [tab, query]);

  return (
    <div className="site">
      <header className="site-header">
        <a className="brand" href="#top" aria-label="Waymark home">
          <span className="brand-mark"><MapPin size={18} /></span>
          <span>Waymark</span>
        </a>
        <nav aria-label="Primary">
          <a href="#demo">Demo</a>
          <a href="#features">Features</a>
          <a href="#widgets">Widgets</a>
          <a href="#data">Data</a>
          <a href="#docs">Docs</a>
          <a className="header-github" href="https://github.com/KusokMedi/waymark" target="_blank" rel="noopener noreferrer">
            <Github size={16} /> GitHub
          </a>
          <a className="btn btn-small" href="https://github.com/KusokMedi/waymark/releases/latest" target="_blank" rel="noopener noreferrer">
            <Download size={15} /> Get the app
          </a>
        </nav>
      </header>

      <main id="top">
        <section className="hero">
          <div className="hero-copy">
            <div className="eyebrow"><Sparkles size={14} /> Independent · Local-first · Open source</div>
            <h1>Know before you go.</h1>
            <p className="lead">
              A native Android watchlist for official U.S. Department of State travel advisories.
              Track the countries you care about, see meaningful changes over time, and keep real
              home-screen widgets — without an account or a Waymark server.
            </p>
            <div className="hero-actions">
              <a className="btn" href="https://github.com/KusokMedi/waymark/releases/latest" target="_blank" rel="noopener noreferrer">
                <Download size={17} /> Download for Android
              </a>
              <a className="btn secondary" href="#demo">
                Try the demo <ArrowRight size={16} />
              </a>
            </div>
            <dl className="hero-facts">
              <div><dt>Minimum</dt><dd>Android 8.0</dd></div>
              <div><dt>Storage</dt><dd>On device</dd></div>
              <div><dt>License</dt><dd>GPL-3.0</dd></div>
              <div><dt>Source</dt><dd>travel.state.gov</dd></div>
            </dl>
          </div>
          <div className="hero-device" aria-hidden="true">
            <div className="phone">
              <div className="phone-notch" />
              <div className="phone-screen">
                <div className="phone-top"><span>Waymark</span><RefreshCw size={14} /></div>
                <div className="phone-banner"><Check size={13} /> Advisories updated</div>
                <div className="mini-card">
                  <span className="mini-flag">FI</span>
                  <div><strong>Finland</strong><small>Exercise Normal Precautions</small><span className="level level-1">Level 1</span></div>
                </div>
                <div className="mini-card">
                  <span className="mini-flag">MX</span>
                  <div><strong>Mexico</strong><small>Contains higher-risk areas</small><span className="level level-2">Level 2</span></div>
                </div>
                <div className="mini-card">
                  <span className="mini-flag">CO</span>
                  <div><strong>Colombia</strong><small>Reconsider Travel</small><span className="level level-3">Level 3</span></div>
                </div>
                <div className="phone-nav"><Star size={15} /><Globe2 size={15} /><History size={15} /><LayoutGrid size={15} /></div>
              </div>
            </div>
          </div>
        </section>

        <section id="demo" className="section demo-section">
          <div className="section-heading">
            <div>
              <div className="eyebrow"><Smartphone size={14} /> Interactive preview</div>
              <h2>See how Waymark feels</h2>
              <p>Explore a sample watchlist and country dashboard. Everything here is demonstration data, not live travel advice.</p>
            </div>
            <span className="sample-badge">Sample data — not live advisories</span>
          </div>
          <div className="demo-shell">
            <div className="demo-app">
              <div className="demo-tabs" role="tablist" aria-label="Demo sections">
                <button role="tab" aria-selected={tab === "watchlist"} className={tab === "watchlist" ? "active" : ""} onClick={() => setTab("watchlist")}>
                  <Star size={15} /> Watchlist
                </button>
                <button role="tab" aria-selected={tab === "explore"} className={tab === "explore" ? "active" : ""} onClick={() => setTab("explore")}>
                  <Globe2 size={15} /> Explore
                </button>
                <input
                  type="search"
                  placeholder="Search sample countries"
                  value={query}
                  onChange={(event) => setQuery(event.target.value)}
                  aria-label="Search sample countries"
                />
              </div>
              <div className="demo-list">
                {visible.map((country) => (
                  <button
                    key={country.id}
                    className={`demo-country ${country.id === selectedId ? "selected" : ""}`}
                    onClick={() => setSelectedId(country.id)}
                  >
                    <span className="demo-flag">{country.iso}</span>
                    <span className="demo-country-text">
                      <strong>{country.name}</strong>
                      <small>{LEVEL_LABELS[country.level]}</small>
                    </span>
                    <span className="demo-country-meta">
                      {country.favorite && <Star size={14} className="favorite-star" aria-label="Favorite sample country" />}
                      <span className={levelClass(country.level)}>L{country.level}</span>
                      <ChevronRight size={16} />
                    </span>
                  </button>
                ))}
                {visible.length === 0 && <p className="demo-empty">No sample country matches that search.</p>}
              </div>
            </div>
            <article className="demo-detail" aria-live="polite">
              <div className="demo-detail-head">
                <span className="demo-flag large">{selected.iso}</span>
                <div>
                  <h3>{selected.name}</h3>
                  <small>ISO {selected.id} · Sample destination</small>
                </div>
              </div>
              <div className={`advisory-banner ${levelClass(selected.level)}`}>
                <strong>Level {selected.level}</strong>
                <span>{LEVEL_LABELS[selected.level]}</span>
                {selected.higherRisk && <em>Contains areas with higher security risk</em>}
              </div>
              <p className="demo-note">{LEVEL_NOTES[selected.level]}</p>
              <dl className="demo-facts">
                <div><dt>Official advisory date</dt><dd>{selected.date}</dd></div>
                <div><dt>Regional note</dt><dd>{selected.region ?? "No separate regional note in this sample"}</dd></div>
                <div><dt>Last sync</dt><dd>Demonstration only</dd></div>
              </dl>
              <p className="demo-disclaimer">
                Sample interface data. Waymark never presents demonstration values as current official advisories.
                The real app retrieves public data from the U.S. Department of State.
              </p>
            </article>
          </div>
        </section>

        <section id="features" className="section">
          <div className="section-heading">
            <div>
              <div className="eyebrow"><Shield size={14} /> Built for the device</div>
              <h2>A watchlist, not another feed</h2>
              <p>Native Android, local history, honest offline states and no reason to create an account.</p>
            </div>
          </div>
          <div className="feature-grid">
            <article className="feature"><Star size={20} /><h3>Your watchlist</h3><p>Choose countries manually. Favorites, ordering and per-country notification preferences stay on your device.</p></article>
            <article className="feature"><Bell size={20} /><h3>Meaningful changes</h3><p>Notifications for worsening, improvement, new regional records and significant detail changes. Never on first sync.</p></article>
            <article className="feature"><LayoutGrid size={20} /><h3>Real app widgets</h3><p>Compact card, list and grid widgets with independent configuration for every instance.</p></article>
            <article className="feature"><History size={20} /><h3>Local history</h3><p>Advisory changes are recorded indefinitely on this device, with a chart built only from recorded transitions.</p></article>
            <article className="feature"><WifiOff size={20} /><h3>Honest offline mode</h3><p>Cached advisories stay available. Failures never become Level 1, and the last successful update is always visible.</p></article>
            <article className="feature"><Lock size={20} /><h3>No tracking</h3><p>No accounts, analytics, telemetry, advertising SDKs or Waymark backend. Only the official public source is contacted.</p></article>
          </div>
        </section>

        <section id="widgets" className="section widgets-section">
          <div className="widget-copy">
            <div className="eyebrow"><LayoutGrid size={14} /> Home screen</div>
            <h2>Advisories where you already look</h2>
            <p>
              Waymark widgets are real Android App Widgets, not web shortcuts. Pick countries, reorder them,
              choose what is visible and how the widget is arranged. Each widget keeps its own configuration,
              even across reboots.
            </p>
            <ul className="check-list">
              <li><Check size={16} /> Compact card, list and multi-country grid layouts</li>
              <li><Check size={16} /> Independent settings for every widget instance</li>
              <li><Check size={16} /> Tap a country to open its dashboard</li>
              <li><Check size={16} /> Updates after synchronization; no per-widget network jobs</li>
              <li><Check size={16} /> Clear empty state when no countries are selected</li>
            </ul>
          </div>
          <div className="widget-preview" aria-label="Illustration of sample Waymark widgets">
            <div className="widget-surface widget-large">
              <div className="widget-head"><strong>Waymark</strong><RefreshCw size={13} /></div>
              <div className="widget-row"><span className="demo-flag small">FI</span><div><strong>Finland</strong><small>Level 1</small></div><span className="level level-1">L1</span></div>
              <div className="widget-row"><span className="demo-flag small">MX</span><div><strong>Mexico</strong><small>Level 2 · higher-risk areas</small></div><span className="level level-2">L2</span></div>
              <div className="widget-row"><span className="demo-flag small">CO</span><div><strong>Colombia</strong><small>Level 3</small></div><span className="level level-3">L3</span></div>
            </div>
            <div className="widget-surface widget-compact">
              <span className="demo-flag small">JP</span>
              <div><strong>Japan</strong><small>Exercise Normal Precautions</small></div>
              <span className="level level-1">L1</span>
            </div>
            <p className="widget-caption">Illustrative widget layouts with sample data. Actual widget rendering follows Android launcher constraints.</p>
          </div>
        </section>

        <section id="screenshots" className="section">
          <div className="section-heading">
            <div>
              <div className="eyebrow"><Smartphone size={14} /> Screenshots</div>
              <h2>Real screenshots, clearly labeled</h2>
              <p>These regions are placeholders until the project owner supplies genuine Android screenshots. They are not fabricated app screens.</p>
            </div>
          </div>
          <div className="screenshot-grid">
            <div className="screenshot-placeholder"><span>Watchlist screenshot placeholder</span><small>Required asset: Android phone, watchlist populated</small></div>
            <div className="screenshot-placeholder"><span>Country dashboard placeholder</span><small>Required asset: detail with level and history chart</small></div>
            <div className="screenshot-placeholder"><span>Widget configuration placeholder</span><small>Required asset: widget settings on a home screen</small></div>
          </div>
        </section>

        <section id="data" className="section data-section">
          <div className="data-copy">
            <div className="eyebrow"><Database size={14} /> Provenance</div>
            <h2>Official data, documented behavior</h2>
            <p>
              Waymark retrieves public travel advisory records from the U.S. Department of State's ArcGIS
              Travel Advisory Levels service. The app preserves the published advisory date separately from
              the local synchronization time.
            </p>
            <div className="level-table">
              <div><strong>Level 1</strong><span>Exercise Normal Precautions</span></div>
              <div><strong>Level 2</strong><span>Exercise Increased Caution</span></div>
              <div><strong>Level 3</strong><span>Reconsider Travel</span></div>
              <div><strong>Level 4</strong><span>Do Not Travel</span></div>
            </div>
            <p className="muted">
              Raw codes 10, 20 and 30 share the base level but mark countries containing areas with higher
              security risk. They are mapped explicitly and never treated as ordinary numeric levels.
            </p>
            <a className="text-link" href="https://travel.state.gov/en/international-travel/travel-advisories.html" target="_blank" rel="noopener noreferrer">
              Read the official advisories <ArrowRight size={15} />
            </a>
          </div>
          <aside className="privacy-card">
            <Lock size={22} />
            <h3>Local by architecture</h3>
            <p>Your watchlist, favorites, history, widget settings and notification preferences never leave the device. There is no cloud sync and no account.</p>
            <p className="muted">The only network request retrieves public advisory data from the official source.</p>
          </aside>
        </section>

        <section id="docs" className="section docs-section">
          <div className="section-heading">
            <div>
              <div className="eyebrow"><Github size={14} /> Open development</div>
              <h2>Documentation and releases</h2>
            </div>
          </div>
          <div className="docs-grid">
            <a href="https://github.com/KusokMedi/waymark/blob/master/docs/ARCHITECTURE.md">Architecture <ArrowRight size={15} /></a>
            <a href="https://github.com/KusokMedi/waymark/blob/master/docs/DATA_SOURCES.md">Data sources <ArrowRight size={15} /></a>
            <a href="https://github.com/KusokMedi/waymark/blob/master/docs/USER_GUIDE.md">User guide <ArrowRight size={15} /></a>
            <a href="https://github.com/KusokMedi/waymark/blob/master/docs/PRIVACY.md">Privacy <ArrowRight size={15} /></a>
            <a href="https://github.com/KusokMedi/waymark/blob/master/CONTRIBUTING.md">Contributing <ArrowRight size={15} /></a>
            <a href="https://github.com/KusokMedi/waymark/blob/master/CHANGELOG.md">Changelog <ArrowRight size={15} /></a>
          </div>
        </section>

        <section id="about" className="section about-section">
          <h2>About Waymark</h2>
          <p>
            Waymark is an independent, open-source project licensed under GPL-3.0. It is not affiliated with,
            endorsed by, or operated by the U.S. Department of State. U.S. Travel Advisories are written for
            U.S. citizens, nationals and legal residents; they are not a universal safety rating for every traveler.
          </p>
          <div className="about-actions">
            <a className="btn" href="https://github.com/KusokMedi/waymark/releases/latest" target="_blank" rel="noopener noreferrer">
              <Download size={16} /> Download the latest release
            </a>
            <a className="btn secondary" href="https://github.com/KusokMedi/waymark" target="_blank" rel="noopener noreferrer">
              <Github size={16} /> View source
            </a>
          </div>
        </section>
      </main>

      <footer className="site-footer">
        <div>
          <strong>Waymark</strong>
          <span>Know before you go.</span>
        </div>
        <p>
          GPL-3.0 · Independent project · Not an official U.S. government product ·{" "}
          <a href="https://travel.state.gov/en/international-travel/travel-advisories.html" target="_blank" rel="noopener noreferrer">Official advisories</a>
        </p>
      </footer>
    </div>
  );
}
