import { describe, expect, it } from "vitest";
import { tasteMatch } from "@/lib/social";

describe("tasteMatch", () => {
  it("is 100 for identical genre sets", () => {
    expect(tasteMatch(["action", "comedy"], ["comedy", "action"])).toBe(100);
  });

  it("is 0 for disjoint sets", () => {
    expect(tasteMatch(["action"], ["drama"])).toBe(0);
  });

  it("is 0 when either side is empty", () => {
    expect(tasteMatch([], ["action"])).toBe(0);
    expect(tasteMatch(["action"], [])).toBe(0);
  });

  it("returns the Jaccard percentage for partial overlap", () => {
    // {action,comedy} vs {action,drama}: intersection 1, union 3 -> 33
    expect(tasteMatch(["action", "comedy"], ["action", "drama"])).toBe(33);
    // {a,b} vs {a,b,c,d}: intersection 2, union 4 -> 50
    expect(tasteMatch(["a", "b"], ["a", "b", "c", "d"])).toBe(50);
  });
});
