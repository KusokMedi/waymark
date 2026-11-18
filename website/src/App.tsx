import { ExternalLink, Github } from "lucide-react";

export default function App() {
  return (
    <div className="site">
      <header className="site-header">
        <div className="brand">Waymark</div>
        <nav>
          <a href="#features">Features</a>
          <a href="#screenshots">Screenshots</a>
          <a href="#data">Data</a>
          <a href="#docs">Docs</a>
          <a href="#about">About</a>
          <a className="cta" href="./app/">Open app</a>
        </nav>
      </header>

      <section className="hero">
        <h1>Waymark</h1>
        <p className="tagline">Know before you go.</p>
        <p className="lead">
          A modern, open-source dashboard for U.S. Department of State travel advisories.
          Browse destinations, understand advisory levels, and keep a personal watchlist.
        </p>
        <div className="hero-actions">
          <a className="btn" href="./app/">Open the dashboard</a>
          <a className="btn secondary" href="https://github.com/KusokMedi/waymark" target="_blank" rel="noopener noreferrer">
            <Github size={16} /> GitHub
          </a>
        </div>
      </section>

      <section id="features" className="section">
        <h2>Features</h2>
        <div className="feature-grid">
          <div className="feature"><h3>Global directory</h3><p>All destinations published by the U.S. Department of State, searchable and filterable.</p></div>
          <div className="feature"><h3>Advisory levels</h3><p>Levels 1–4 plus special higher-risk-area indicators, preserved exactly as published.</p></div>
          <div className="feature"><h3>List &amp; card views</h3><p>Switch between compact and visual presentation. Preference is saved locally.</p></div>
          <div className="feature"><h3>Destination details</h3><p>Official description, regional records, departure notices, and links to the original advisory.</p></div>
          <div className="feature"><h3>Favorites &amp; watchlist</h3><p>Keep the destinations you care about. Stored in your browser, no account required.</p></div>
          <div className="feature"><h3>Themes</h3><p>System, light, and dark themes with no flash on load.</p></div>
          <div className="feature"><h3>Offline resilience</h3><p>Last known good data is cached and clearly marked when stale.</p></div>
          <div className="feature"><h3>Automatic updates</h3><p>Scheduled synchronization keeps the published snapshot current.</p></div>
        </div>
      </section>

      <section id="screenshots" className="section">
        <h2>Screenshots</h2>
        <p className="muted">
          Genuine application screenshots will be added here. Placeholders are shown until then.
        </p>
        <div className="screenshot-grid">
          <div className="screenshot-placeholder">Dashboard screenshot placeholder</div>
          <div className="screenshot-placeholder">Destination list placeholder</div>
          <div className="screenshot-placeholder">Destination details placeholder</div>
        </div>
      </section>

      <section id="data" className="section">
        <h2>Data sources</h2>
        <p>
          Waymark displays travel advisories published by the U.S. Department of State.
          Structured data is retrieved from the official ArcGIS Travel Advisory Levels layer.
        </p>
        <h3>Advisory levels</h3>
        <ul className="levels">
          <li><strong>Level 1</strong> — Exercise Normal Precautions</li>
          <li><strong>Level 2</strong> — Exercise Increased Caution</li>
          <li><strong>Level 3</strong> — Reconsider Travel</li>
          <li><strong>Level 4</strong> — Do Not Travel</li>
        </ul>
        <p>
          Codes 10, 20, and 30 indicate the same base level but with areas of higher security risk. These are preserved
          separately and are not treated as ordinary levels.
        </p>
        <p className="muted">
          Advisories are written for U.S. citizens, nationals, and legal residents. They are not a universal safety
          assessment for travelers of every nationality.
        </p>
        <a href="https://travel.state.gov/en/international-travel/travel-advisories.html" target="_blank" rel="noopener noreferrer">
          Official travel advisories <ExternalLink size={14} />
        </a>
      </section>

      <section id="docs" className="section">
        <h2>Documentation</h2>
        <ul>
          <li><a href="https://github.com/KusokMedi/waymark/blob/master/docs/ARCHITECTURE.md">Architecture</a></li>
          <li><a href="https://github.com/KusokMedi/waymark/blob/master/docs/DATA_SOURCES.md">Data sources</a></li>
          <li><a href="https://github.com/KusokMedi/waymark/blob/master/docs/DEVELOPMENT.md">Development</a></li>
          <li><a href="https://github.com/KusokMedi/waymark/blob/master/docs/RELEASES.md">Releases</a></li>
          <li><a href="https://github.com/KusokMedi/waymark/blob/master/CONTRIBUTING.md">Contributing</a></li>
          <li><a href="https://github.com/KusokMedi/waymark/blob/master/CHANGELOG.md">Changelog</a></li>
        </ul>
      </section>

      <section id="changelog" className="section">
        <h2>Changelog</h2>
        <p>See the <a href="https://github.com/KusokMedi/waymark/blob/master/CHANGELOG.md">CHANGELOG.md</a> for release history.</p>
      </section>

      <section id="about" className="section">
        <h2>About</h2>
        <p>
          Waymark is an independent open-source project licensed under GPL-3.0. It is not an official U.S. Department of
          State product and is not affiliated with or endorsed by the U.S. Department of State.
        </p>
      </section>

      <footer className="site-footer">
        <p>© Waymark contributors. GPL-3.0.</p>
      </footer>
    </div>
  );
}
