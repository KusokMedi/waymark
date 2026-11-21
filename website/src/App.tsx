import { useState } from "react";
import {
  ArrowRight,
  Bell,
  Check,
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
  Sparkles,
  Star,
  WifiOff,
} from "lucide-react";

const DEMO_ITEMS = [
  { id: "FIN", name: "Finland", iso2: "FI", level: 1, higher: false, date: "2026-03-13", regions: [] as string[] },
  { id: "MEX", name: "Mexico", iso2: "MX", level: 2, higher: true, date: "2026-02-01", regions: ["Jalisco", "Yucatan"] },
  { id: "COL", name: "Colombia", iso2: "CO", level: 3, higher: false, date: "2026-01-15", regions: ["Border regions"] },
  { id: "DEU", name: "Germany", iso2: "DE", level: 1, higher: false, date: "2026-03-20", regions: [] as string[] },
  { id: "BRA", name: "Brazil", iso2: "BR", level: 2, higher: false, date: "2026-02-20", regions: ["Some states"] },
];

export default function App() {
  const [menuOpen, setMenuOpen] = useState(false);
  const [demoTab, setDemoTab] = useState<"watchlist" | "explore">("watchlist");
  const [demoQuery, setDemoQuery] = useState("");
  const [demoSelected, setDemoSelected] = useState("MEX");
  const demoBase = demoTab === "watchlist" ? DEMO_ITEMS.slice(0, 3) : DEMO_ITEMS;
  const filteredDemo = demoBase.filter(
    (item) =>
      item.name.toLowerCase().includes(demoQuery.toLowerCase()) ||
      item.iso2.toLowerCase().includes(demoQuery.toLowerCase()),
  );
  const demoItem = DEMO_ITEMS.find((item) => item.id === demoSelected) ?? null;

  return (
    <div className="site">
      <header className="site-header">
        <a className="brand" href="#top" aria-label="Waymark home">
          <span className="brand-mark"><MapPin size={18} /></span>
          <span>Waymark</span>
        </a>
        <button
          className={`menu-toggle ${menuOpen ? "open" : ""}`}
          type="button"
          aria-expanded={menuOpen}
          aria-controls="primary-nav"
          aria-label={menuOpen ? "Close menu" : "Open menu"}
          onClick={() => setMenuOpen((value) => !value)}
        >
          <span className="bar" />
          <span className="bar" />
          <span className="bar" />
        </button>
        <nav
          id="primary-nav"
          className={menuOpen ? "open" : ""}
          aria-label="Primary"
          onClick={() => setMenuOpen(false)}
        >
          <a href="#features">Features</a>
          <a href="#widgets">Widgets</a>
          <a href="#demo">Demo</a>
          <a href="#data">Data</a>
          <a href="#docs">Docs</a>
          <a className="nav-download" href="https://github.com/KusokMedi/waymark/releases/latest" target="_blank" rel="noopener noreferrer">Download</a>
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
              home-screen widgets. No account, no Waymark server, no analytics, no tracking.
            </p>
            <div className="hero-actions">
              <a className="btn" href="https://github.com/KusokMedi/waymark/releases/latest" target="_blank" rel="noopener noreferrer">
                <Download size={17} /> Download for Android
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
            <article className="feature"><LayoutGrid size={20} /><h3>Real app widgets</h3><p>A watchlist widget with flags, advisory levels, and last update. Resizable, with an empty state and automatic refresh.</p></article>
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
              Waymark widgets are real Android App Widgets, not web shortcuts. The watchlist widget shows your
              selected countries with flags, advisory levels, and the last update time. It is resizable and
              refreshes automatically after synchronization.
            </p>
            <ul className="check-list">
              <li><Check size={16} /> Watchlist widget with country flags, advisory levels, and last update</li>
              <li><Check size={16} /> Resizable from 2 by 2 down to 2 by 1</li>
              <li><Check size={16} /> Tap a country to open its dashboard</li>
              <li><Check size={16} /> Automatic refresh after synchronization</li>
              <li><Check size={16} /> Clear empty state when no countries are selected</li>
            </ul>
          </div>
          <div className="widget-preview" aria-label="Illustration of sample Waymark widgets">
            <div className="widget-surface widget-large">
              <div className="widget-head"><strong>Waymark</strong><RefreshCw size={13} /></div>
              <div className="widget-row"><span className="demo-flag small">FI</span><div><strong>Finland</strong></div><span className="level level-1">L1</span></div>
              <div className="widget-row"><span className="demo-flag small">MX</span><div><strong>Mexico</strong></div><span className="level level-2">L2</span></div>
              <div className="widget-row"><span className="demo-flag small">CO</span><div><strong>Colombia</strong></div><span className="level level-3">L3</span></div>
            </div>
            <div className="widget-surface widget-compact">
              <span className="demo-flag small">FI</span>
              <div><strong>Finland</strong></div>
              <span className="level level-1">L1</span>
            </div>
            <p className="widget-caption">Illustrative widget layouts with sample data. Actual widget rendering follows Android launcher constraints.</p>
          </div>
        </section>

        <section id="demo" className="section">
          <div className="section-heading">
            <div>
              <div className="eyebrow"><Sparkles size={14} /> Sample data</div>
              <h2>Try the interface</h2>
              <p>Interactive demonstration using clearly labeled sample data. Not real current travel advice.</p>
            </div>
          </div>
          <div className="demo-shell">
            <div className="demo-app">
              <div className="demo-tabs">
                <button className={demoTab === "watchlist" ? "active" : ""} type="button" onClick={() => setDemoTab("watchlist")}>Watchlist</button>
                <button className={demoTab === "explore" ? "active" : ""} type="button" onClick={() => setDemoTab("explore")}>Explore</button>
                <input type="search" placeholder="Search sample destinations" value={demoQuery} onChange={(e) => setDemoQuery(e.target.value)} aria-label="Search sample destinations" />
              </div>
              <div className="demo-list">
                {filteredDemo.length === 0 ? (
                  <div className="demo-empty">No sample destinations match.</div>
                ) : (
                  filteredDemo.map((item) => (
                    <button key={item.id} type="button" className={`demo-country ${demoSelected === item.id ? "selected" : ""}`} onClick={() => setDemoSelected(item.id)}>
                      <span className="demo-flag small">{item.iso2}</span>
                      <div className="demo-country-text">
                        <strong>{item.name}</strong>
                        <small>{item.regions.length ? item.regions.join(", ") : "Country advisory"}</small>
                      </div>
                      <span className="demo-country-meta">
                        <span className={`level level-${item.level}`}>L{item.level}</span>
                      </span>
                    </button>
                  ))
                )}
              </div>
            </div>
            <div className="demo-detail">
              {demoItem ? (
                <>
                  <div className="demo-detail-head">
                    <span className="demo-flag large">{demoItem.iso2}</span>
                    <div>
                      <h3>{demoItem.name}</h3>
                      <small>{demoItem.iso2} / {demoItem.id}</small>
                    </div>
                  </div>
                  <div className={`advisory-banner level-${demoItem.level}`}>
                    <strong>Level {demoItem.level}</strong>
                    <span>{demoItem.higher ? "Contains areas with higher security risk." : "No higher-risk areas noted."}</span>
                    <em>Sample advisory date: {demoItem.date}</em>
                  </div>
                  <p className="demo-note">Sample record. Real advisories come from the official U.S. Department of State source.</p>
                  <dl className="demo-facts">
                    <div><dt>Regions</dt><dd>{demoItem.regions.length ? demoItem.regions.join(", ") : "None"}</dd></div>
                    <div><dt>Sample advisory date</dt><dd>{demoItem.date}</dd></div>
                  </dl>
                  <p className="demo-disclaimer">Sample data only. Not real current travel advice.</p>
                </>
              ) : (
                <div className="demo-empty">Select a sample destination.</div>
              )}
            </div>
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
