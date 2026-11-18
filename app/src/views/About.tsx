
export default function About() {
  return (
    <div>
      <h1>About Waymark</h1>
      <div className="card" style={{ marginBottom: "var(--space-4)" }}>
        <p>
          Waymark is an independent, open-source dashboard for travel advisories published by the U.S. Department of State.
        </p>
        <p>
          It is not an official U.S. government product and is not affiliated with, endorsed by, or operated by the U.S. Department of State.
        </p>
        <h2>Important limitation</h2>
        <p>
          U.S. Department of State Travel Advisories are assessments of risks and recommended precautions for U.S. citizens, nationals, and legal residents.
          They do not necessarily represent the risk assessment applicable to travelers of every nationality.
        </p>
        <h2>Data</h2>
        <p>
          Advisory levels, descriptions, dates, and links are sourced from the U.S. Department of State via a public ArcGIS feature layer.
          Special codes indicating areas with higher security risk are preserved separately from ordinary advisory levels.
        </p>
        <h2>License</h2>
        <p>Waymark is licensed under the GNU General Public License v3.0.</p>
      </div>
    </div>
  );
}
