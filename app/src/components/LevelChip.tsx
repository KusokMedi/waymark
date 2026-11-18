import { LEVEL_LABELS, type AdvisoryLevel } from "../data/model";

export function levelClass(level: AdvisoryLevel | null, higherRisk: boolean): string {
  if (level === null) return "level-unknown";
  const base = `level-${level}`;
  return higherRisk ? `${base} level-higher` : base;
}

export function LevelChip({
  level,
  higherRisk,
  label,
}: {
  level: AdvisoryLevel | null;
  higherRisk?: boolean;
  label?: string | null;
}) {
  const hr = higherRisk ?? false;
  const text =
    label ??
    (level !== null ? LEVEL_LABELS[level] + (hr ? " — Contains Areas with Higher Security Risk" : "") : "Unknown advisory");
  const cls = level === null ? "level-unknown" : `level-${level}${hr ? " level-higher" : ""}`;
  return <span className={`level-chip ${cls}`}>{text}</span>;
}
