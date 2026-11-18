import { Globe } from "lucide-react";

function alpha2ToFlagEmoji(code: string | null): string | null {
  if (!code || code.length !== 2) return null;
  const c = code.toUpperCase();
  if (!/^[A-Z]{2}$/.test(c)) return null;
  // Regional indicator symbols
  const A = 0x1f1e6;
  const base = "A".charCodeAt(0);
  return String.fromCodePoint(A + (c.charCodeAt(0) - base)) + String.fromCodePoint(A + (c.charCodeAt(1) - base));
}

export function Flag({ isoAlpha2, name }: { isoAlpha2: string | null; name: string }) {
  const emoji = alpha2ToFlagEmoji(isoAlpha2);
  if (emoji) {
    return (
      <span className="flag" role="img" aria-label={`${name} flag`} style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", fontSize: "1rem", background: "transparent" }}>
        {emoji}
      </span>
    );
  }
  return (
    <span className="fallback-icon" aria-label={`${name} (no flag available)`}>
      <Globe size={16} />
    </span>
  );
}
