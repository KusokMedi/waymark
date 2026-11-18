import { describe, it, expect } from "vitest";
import { normalizeFeature, normalizeFeatures, groupIntoDestinations } from "./normalize";
import { RAW_CODE_MAP } from "./model";

function feat(overrides: Record<string, unknown>) {
  return { attributes: { OBJECTID: 1, GlobalID: "g", ...overrides } };
}

describe("normalizeFeature", () => {
  it("maps each known raw code", () => {
    for (const code of [1, 10, 2, 20, 3, 30, 4]) {
      const rec = normalizeFeature(feat({ NAME: "X", LEVEL_: code, ISO_3: "FIN" }), 0);
      expect(rec).not.toBeNull();
      const mapped = RAW_CODE_MAP[code];
      expect(rec!.level).toBe(mapped.level);
      expect(rec!.containsHigherRiskAreas).toBe(mapped.higherRisk);
      expect(rec!.officialLabel).toBe(mapped.label);
      expect(rec!.rawLevelCode).toBe(code);
    }
  });

  it("returns null for missing name", () => {
    expect(normalizeFeature(feat({ NAME: null, ISO_3: "FIN" }), 0)).toBeNull();
  });

  it("preserves unknown code without forcing level", () => {
    const rec = normalizeFeature(feat({ NAME: "X", LEVEL_: 99, ISO_3: "FIN" }), 0);
    expect(rec!.level).toBeNull();
    expect(rec!.rawLevelCode).toBe(99);
    expect(rec!.officialLabel).toContain("Unsupported");
  });

  it("handles null level (missing advisory)", () => {
    const rec = normalizeFeature(feat({ NAME: "American Samoa", LEVEL_: null, ISO_3: "ASM", URL: " ", ADVDATE: null }), 0);
    expect(rec!.level).toBeNull();
    expect(rec!.rawLevelCode).toBeNull();
    expect(rec!.advisoryUrl).toBeNull();
    expect(rec!.advisoryDate).toBeNull();
  });

  it("converts ms timestamp to UTC date", () => {
    const rec = normalizeFeature(feat({ NAME: "Finland", LEVEL_: 1, ISO_3: "FIN", ADVDATE: 1773374400000 }), 0);
    expect(rec!.advisoryDate).toBe("2026-03-13");
  });

  it("validates urls", () => {
    const rec = normalizeFeature(feat({ NAME: "X", LEVEL_: 1, ISO_3: "FIN", URL: "not-a-url" }), 0);
    expect(rec!.advisoryUrl).toBeNull();
  });

  it("resolves ISO alpha-2", () => {
    const rec = normalizeFeature(feat({ NAME: "Finland", LEVEL_: 1, ISO_3: "FIN" }), 0);
    expect(rec!.isoAlpha2).toBe("FI");
  });
});

describe("groupIntoDestinations", () => {
  it("groups Mexico states under one destination", () => {
    const features = [
      feat({ OBJECTID: 1, NAME: "Mexico", LEVEL_: null, ISO_3: "MEX", ADVDATE: 1780081872000 }),
      feat({ OBJECTID: 2, NAME: "Jalisco", LEVEL_: 3, ISO_3: "MEX", ADVDATE: 1692745200000 }),
      feat({ OBJECTID: 3, NAME: "Yucatán", LEVEL_: 1, ISO_3: "MEX", ADVDATE: 1692745200000 }),
    ];
    const records = normalizeFeatures(features);
    const dests2 = groupIntoDestinations(records);
    const mex = dests2.find((d) => d.isoAlpha3 === "MEX");
    expect(mex).toBeDefined();
    expect(mex!.records.length).toBe(3);
  });

  it("does not default missing to level 1", () => {
    const features = [feat({ NAME: "American Samoa", LEVEL_: null, ISO_3: "ASM" })];
    const records = normalizeFeatures(features);
    const dests = groupIntoDestinations(records);
    expect(dests[0].level).toBeNull();
  });
});
