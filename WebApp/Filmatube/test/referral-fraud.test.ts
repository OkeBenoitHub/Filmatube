import { describe, expect, it } from "vitest";
import { classifyReferrals, type ReferralSignal } from "@/lib/admin/referral-fraud";

const r = (referredId: string, referrerId: string, ipHash = ""): ReferralSignal => ({ referredId, referrerId, ipHash });

describe("referral fraud classification", () => {
  it("passes clean referrals from distinct networks", () => {
    const out = classifyReferrals([r("a", "boss", "ip1"), r("b", "boss", "ip2")]);
    expect(out.get("a")).toBe("");
    expect(out.get("b")).toBe("");
  });

  it("flags self-referral", () => {
    expect(classifyReferrals([r("x", "x", "ip1")]).get("x")).toBe("self-referral");
  });

  it("flags a referrer whose referred accounts share a signup IP", () => {
    const out = classifyReferrals([r("a", "farmer", "sameip"), r("b", "farmer", "sameip"), r("c", "farmer", "other")]);
    expect(out.get("a")).toBe("shared signup IP");
    expect(out.get("b")).toBe("shared signup IP");
    expect(out.get("c")).toBe(""); // different network → clean
  });

  it("does NOT flag a lone account on a shared IP (households / campus)", () => {
    // Two different referrers, each with one signup from the same network — legitimate.
    const out = classifyReferrals([r("a", "alice", "sharedip"), r("b", "bob", "sharedip")]);
    expect(out.get("a")).toBe("");
    expect(out.get("b")).toBe("");
  });

  it("ignores an empty ip hash (no signal captured)", () => {
    const out = classifyReferrals([r("a", "boss", ""), r("b", "boss", "")]);
    expect(out.get("a")).toBe("");
    expect(out.get("b")).toBe("");
  });
});
