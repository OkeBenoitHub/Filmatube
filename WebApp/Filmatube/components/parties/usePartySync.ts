"use client";

import { useCallback, useEffect, useRef, useState, type RefObject } from "react";
import { doc, onSnapshot, serverTimestamp, setDoc, type Timestamp } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/components/providers/AuthProvider";

/** Guests hard-seek past this much drift — matches Android PARTY_DRIFT_TOLERANCE_MS. */
const DRIFT_TOLERANCE_MS = 2_500;
/** Host heartbeat / guest re-check cadence — matches Android PARTY_HEARTBEAT_MS. */
const HEARTBEAT_MS = 5_000;

interface SyncState {
  positionMs: number;
  isPlaying: boolean;
  updatedAtMs: number;
}

export interface PartySync {
  isParty: boolean;
  isHost: boolean;
  ended: boolean;
  /** Autoplay was blocked — the guest must click once to join playback. */
  needsGesture: boolean;
  joinPlayback: () => void;
}

/**
 * Client half of the watch-party sync engine — the same contract as Android.
 *
 * Host: publishes {positionMs, isPlaying, updatedAt} on play/pause/seek plus a 5s heartbeat.
 * Guests: extrapolate `positionMs + (now - updatedAt)` while playing and hard-seek past 2.5s
 * of drift, re-checked every 5s. Guest catch-up falls out of the same maths.
 */
export function usePartySync(
  partyId: string | null,
  videoRef: RefObject<HTMLVideoElement | null>,
): PartySync {
  const { user } = useAuth();
  const [isHost, setIsHost] = useState(false);
  const [ended, setEnded] = useState(false);
  const [needsGesture, setNeedsGesture] = useState(false);
  const last = useRef<SyncState | null>(null);

  // Room state: who hosts (handoff moves this live) and whether it's over.
  useEffect(() => {
    if (!partyId || !user) return;
    return onSnapshot(doc(db, "parties", partyId), (snap) => {
      if (!snap.exists()) return;
      setIsHost(snap.get("hostId") === user.uid);
      if (snap.get("status") === "ended") {
        setEnded(true);
        videoRef.current?.pause();
      }
    });
  }, [partyId, user, videoRef]);

  const publish = useCallback(async () => {
    const v = videoRef.current;
    if (!partyId || !v) return;
    try {
      await setDoc(doc(db, "parties", partyId, "sync", "state"), {
        positionMs: Math.floor(v.currentTime * 1000),
        isPlaying: !v.paused,
        updatedAt: serverTimestamp(),
      });
    } catch {
      /* rules reject non-hosts; nothing to do */
    }
  }, [partyId, videoRef]);

  // HOST: mirror every transport change to the room, plus a heartbeat while playing.
  useEffect(() => {
    const v = videoRef.current;
    if (!partyId || !isHost || !v) return;
    const onChange = () => void publish();
    v.addEventListener("play", onChange);
    v.addEventListener("pause", onChange);
    v.addEventListener("seeked", onChange);
    const beat = setInterval(() => {
      if (!v.paused) void publish();
    }, HEARTBEAT_MS);
    return () => {
      v.removeEventListener("play", onChange);
      v.removeEventListener("pause", onChange);
      v.removeEventListener("seeked", onChange);
      clearInterval(beat);
    };
  }, [partyId, isHost, publish, videoRef]);

  const apply = useCallback(
    (st: SyncState) => {
      const v = videoRef.current;
      if (!v) return;
      if (!st.isPlaying && !v.paused) v.pause();
      if (st.isPlaying && v.paused) {
        // Browsers block play() without a user gesture — surface a one-tap prompt rather
        // than leaving a guest stuck staring at a paused frame (they have no transport).
        v.play().then(() => setNeedsGesture(false)).catch(() => setNeedsGesture(true));
      }
      const expected = st.isPlaying ? st.positionMs + (Date.now() - st.updatedAtMs) : st.positionMs;
      if (Math.abs(expected - v.currentTime * 1000) > DRIFT_TOLERANCE_MS) {
        v.currentTime = Math.max(0, expected / 1000);
      }
    },
    [videoRef],
  );

  // GUEST: follow the host's writes, and re-check drift between them.
  useEffect(() => {
    if (!partyId || isHost) return;
    const unsub = onSnapshot(doc(db, "parties", partyId, "sync", "state"), (snap) => {
      if (!snap.exists()) return;
      const st: SyncState = {
        positionMs: (snap.get("positionMs") as number) ?? 0,
        isPlaying: (snap.get("isPlaying") as boolean) ?? false,
        // Pending server timestamps read null — treat as now so extrapolation still works.
        updatedAtMs: (snap.get("updatedAt") as Timestamp | null)?.toMillis?.() ?? Date.now(),
      };
      last.current = st;
      apply(st);
    });
    const tick = setInterval(() => {
      if (last.current) apply(last.current);
    }, HEARTBEAT_MS);
    return () => {
      unsub();
      clearInterval(tick);
    };
  }, [partyId, isHost, apply]);

  const joinPlayback = useCallback(() => {
    setNeedsGesture(false);
    if (last.current) apply(last.current);
  }, [apply]);

  return { isParty: !!partyId, isHost, ended, needsGesture, joinPlayback };
}
