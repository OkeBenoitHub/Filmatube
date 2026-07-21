"use client";

import { useCallback, useEffect, useRef, useState, type RefObject } from "react";
import {
  collection,
  deleteDoc,
  doc,
  getDocFromServer,
  onSnapshot,
  serverTimestamp,
  setDoc,
  type Timestamp,
} from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/components/providers/AuthProvider";

/** Hard-seek past this much drift — matches Android PARTY_DRIFT_TOLERANCE_MS, reused here. */
const DRIFT_TOLERANCE_MS = 2_500;
/** Re-check cadence — matches Android THEATER_SYNC_INTERVAL_MS. */
const SYNC_INTERVAL_MS = 5_000;
/** Presence re-stamp cadence — matches Android PRESENCE_HEARTBEAT_MS. */
const PRESENCE_HEARTBEAT_MS = 30_000;
/** Presence goes stale this long after its last beat — matches Android. */
const PRESENCE_STALE_AFTER_MS = 90_000;
/** Past this far into the film, arriving counts as joining mid-show. */
const JOIN_MID_SHOW_THRESHOLD_MS = 60_000;

export interface TheaterSync {
  isTheater: boolean;
  ended: boolean;
  /** Autoplay was blocked — the viewer must click once to join. */
  needsGesture: boolean;
  joinPlayback: () => void;
  /** Non-null when this viewer walked into a showing already in progress. */
  joinedMidShowAtMs: number | null;
  dismissJoinedMidShow: () => void;
  /** How many people are in the room right now (fresh heartbeats only). */
  presentCount: number;
}

/**
 * Client half of the theater sync engine — the web twin of Android's Day 158 engine.
 *
 * Unlike a watch party there is no host: a public screening runs on the wall clock, so every
 * viewer independently computes `serverNow - startAt` and converges on it. Catch-up falls out
 * of the same maths as ordinary drift correction — a late joiner's first convergence pass IS
 * catch-up — and it costs zero writes per viewer, just one read of the showtime doc.
 */
export function useTheaterSync(
  showtimeId: string | null,
  initialStartAtMs: number,
  videoRef: RefObject<HTMLVideoElement | null>,
): TheaterSync {
  const { user } = useAuth();
  const [ended, setEnded] = useState(false);
  const [needsGesture, setNeedsGesture] = useState(false);
  const [joinedMidShowAtMs, setJoinedMidShowAtMs] = useState<number | null>(null);
  const [presentCount, setPresentCount] = useState(0);
  const offsetRef = useRef(0);
  const liveRef = useRef(false);
  const convergedRef = useRef(false);
  // Both live in refs, not props: an admin can shift the schedule (pause/resume/skip) at any
  // moment, and the snapshot below is the authority from then on — not the server render.
  const startAtRef = useRef(initialStartAtMs);
  const pausedAtRef = useRef(0);

  const converge = useCallback(() => {
    const v = videoRef.current;
    if (!v) return;

    // Doors open, film not rolling: hold at the top, paused. Returning early here instead
    // would let the <video> keep playing from 0:00 through the whole lobby — the audience
    // would watch the opening minutes before the screening officially starts.
    if (!liveRef.current) {
      if (!v.paused) v.pause();
      if (v.currentTime > 0) v.currentTime = 0;
      return;
    }

    // An admin pause freezes the effective clock; resuming shifts startAt forward by the
    // paused duration, so this stays a pure function of the two fields.
    const effectiveNow = pausedAtRef.current > 0 ? pausedAtRef.current : Date.now() + offsetRef.current;
    const expectedMs = Math.max(0, effectiveNow - startAtRef.current);

    // Paused by an admin: hold exactly where the room froze rather than drifting on.
    if (pausedAtRef.current > 0) {
      if (!v.paused) v.pause();
      if (Math.abs(expectedMs - v.currentTime * 1000) > DRIFT_TOLERANCE_MS) {
        v.currentTime = expectedMs / 1000;
      }
      return;
    }

    // First convergence of the session: if the film is already well underway, say so rather
    // than silently dropping the viewer into the middle of a scene.
    if (!convergedRef.current) {
      convergedRef.current = true;
      if (expectedMs > JOIN_MID_SHOW_THRESHOLD_MS) setJoinedMidShowAtMs(expectedMs);
    }

    // The schedule has run past the end of the film: the showing is over in practice.
    if (v.duration && expectedMs / 1000 >= v.duration) {
      v.pause();
      return;
    }
    if (v.paused) {
      v.play().then(() => setNeedsGesture(false)).catch(() => setNeedsGesture(true));
    }
    if (Math.abs(expectedMs - v.currentTime * 1000) > DRIFT_TOLERANCE_MS) {
      v.currentTime = expectedMs / 1000;
    }
    // Schedule fields are read through refs, so this stays stable across admin edits —
    // rebuilding it would restart the effect below and re-run the clock probe each time.
  }, [videoRef]);

  // Room status — lobby→live and →ended both have to take effect live, without a reload.
  // Converging straight off the snapshot (rather than waiting for the next interval tick)
  // means the film starts the moment the doors open, not up to SYNC_INTERVAL_MS later.
  useEffect(() => {
    if (!showtimeId) return;
    return onSnapshot(doc(db, "showtimes", showtimeId), (snap) => {
      if (!snap.exists()) return;
      const status = snap.get("status") as string;
      liveRef.current = status === "live";
      startAtRef.current = (snap.get("startAt") as Timestamp | null)?.toMillis?.() ?? startAtRef.current;
      pausedAtRef.current = (snap.get("pausedAt") as Timestamp | null)?.toMillis?.() ?? 0;
      if (status === "ended") {
        setEnded(true);
        videoRef.current?.pause();
        return;
      }
      converge();
    });
  }, [showtimeId, videoRef, converge]);

  // Measure this device's clock skew once, then start converging. Sequenced (not parallel)
  // so the first correction uses the real offset rather than jumping twice in a row.
  useEffect(() => {
    if (!showtimeId || !user) return;
    let cancelled = false;
    let interval: ReturnType<typeof setInterval> | undefined;

    (async () => {
      // Probed against a scratch doc under this user, not the showtime: writing into the
      // showtime's subtree would create attendance as a side effect of merely opening the
      // room, silently inflating attendeesCount via the sync Cloud Function.
      const ref = doc(db, "users", user.uid, "settings", "clockProbe");
      const before = Date.now();
      try {
        await setDoc(ref, { clockProbeAt: serverTimestamp() }, { merge: true });
        const snap = await getDocFromServer(ref);
        const after = Date.now();
        const serverMs = (snap.get("clockProbeAt") as Timestamp | null)?.toMillis?.();
        // The write landed somewhere inside [before, after]; midpoint is the best estimate.
        if (serverMs) offsetRef.current = serverMs - (before + after) / 2;
      } catch {
        /* fall back to the unadjusted local clock */
      }
      if (cancelled) return;
      converge();
      interval = setInterval(converge, SYNC_INTERVAL_MS);
    })();

    return () => {
      cancelled = true;
      if (interval) clearInterval(interval);
    };
  }, [showtimeId, user, converge]);

  // Presence: "I'm in this room right now". Its own subcollection, separate from attendees —
  // an RSVP is intent, presence is fact, and merging them would let merely opening the room
  // inflate the attendee count people see before it starts.
  useEffect(() => {
    if (!showtimeId || !user) return;
    const ref = doc(db, "showtimes", showtimeId, "presence", user.uid);

    const beat = () => void setDoc(ref, { userId: user.uid, presentAt: serverTimestamp() }).catch(() => {});
    beat();
    const interval = setInterval(beat, PRESENCE_HEARTBEAT_MS);

    // Leaving the tab shouldn't wait out the 90s staleness window. `pagehide` is the one
    // teardown hook that fires reliably on mobile Safari, where `beforeunload` does not.
    const leave = () => void deleteDoc(ref).catch(() => {});
    window.addEventListener("pagehide", leave);

    return () => {
      clearInterval(interval);
      window.removeEventListener("pagehide", leave);
      leave();
    };
  }, [showtimeId, user]);

  // Who else is here. Freshness is judged against the cutoff on each snapshot rather than by
  // a query bound, since the cutoff moves every second and would force constant re-subscribes.
  useEffect(() => {
    if (!showtimeId) return;
    return onSnapshot(collection(db, "showtimes", showtimeId, "presence"), (snap) => {
      const cutoff = Date.now() - PRESENCE_STALE_AFTER_MS;
      setPresentCount(
        snap.docs.filter(
          (d) => ((d.get("presentAt") as Timestamp | null)?.toMillis?.() ?? 0) > cutoff,
        ).length,
      );
    });
  }, [showtimeId]);

  const joinPlayback = useCallback(() => {
    setNeedsGesture(false);
    converge();
  }, [converge]);

  const dismissJoinedMidShow = useCallback(() => setJoinedMidShowAtMs(null), []);

  return {
    isTheater: !!showtimeId,
    ended,
    needsGesture,
    joinPlayback,
    joinedMidShowAtMs,
    dismissJoinedMidShow,
    presentCount,
  };
}
