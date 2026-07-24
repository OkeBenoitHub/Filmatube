"use client";

import { useEffect } from "react";
import { REF_COOKIE, REF_COOKIE_MAX_AGE } from "@/lib/referral-shared";

/**
 * Captures an invite code in a cookie so the signup that follows can be attributed server-side.
 * Renders nothing — the visible invite landing is the surrounding server component.
 */
export function SetRefCookie({ code }: { code: string }) {
  useEffect(() => {
    if (!code) return;
    document.cookie = `${REF_COOKIE}=${encodeURIComponent(code)}; max-age=${REF_COOKIE_MAX_AGE}; path=/; samesite=lax`;
  }, [code]);
  return null;
}
