"use client";

import { useCallback, useEffect, useRef, useState, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import {
  Play,
  Pause,
  Volume2,
  VolumeX,
  Maximize,
  Minimize,
  Loader2,
  PictureInPicture2,
  ArrowLeft,
  X,
} from "lucide-react";
import { doc, getDoc, serverTimestamp, setDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/components/providers/AuthProvider";
import type { ActiveMovie } from "@/components/player/MiniPlayerProvider";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import { cn } from "@/lib/utils";

const COMPLETE_THRESHOLD = 0.9;

function formatTime(seconds: number): string {
  if (!Number.isFinite(seconds) || seconds < 0) seconds = 0;
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = Math.floor(seconds % 60);
  const mm = h > 0 ? String(m).padStart(2, "0") : String(m);
  return `${h > 0 ? `${h}:` : ""}${mm}:${String(s).padStart(2, "0")}`;
}

/**
 * The single persistent <video>. Rendered full-screen when `minimized` is false
 * (on /watch/[id]) and as a floating mini-player otherwise; the same element keeps
 * playing across the transition. Streams a token-protected R2 URL and syncs
 * watch-progress with Android via Firestore.
 */
export function PersistentPlayer({
  active,
  minimized,
  dict,
  onClose,
}: {
  active: ActiveMovie;
  minimized: boolean;
  dict: Dictionary["player"];
  onClose: () => void;
}) {
  const router = useRouter();
  const { user } = useAuth();
  const uid = user?.uid;
  const movieId = active.id;

  const videoRef = useRef<HTMLVideoElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const hideTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const appliedResume = useRef(false);

  const [src, setSrc] = useState<string | null>(null);
  const [error, setError] = useState(false);
  const [playing, setPlaying] = useState(false);
  const [buffering, setBuffering] = useState(false);
  const [current, setCurrent] = useState(0);
  const [duration, setDuration] = useState(0);
  const [volume, setVolume] = useState(1);
  const [muted, setMuted] = useState(false);
  const [fullscreen, setFullscreen] = useState(false);
  const [controlsVisible, setControlsVisible] = useState(true);
  const [resumeMs, setResumeMs] = useState<number | null>(null);

  // --- stream source ---
  const load = useCallback(async () => {
    setError(false);
    setSrc(null);
    try {
      const res = await fetch(`/api/stream/${movieId}`);
      if (!res.ok) throw new Error("stream");
      const data = (await res.json()) as { url: string };
      setSrc(data.url);
    } catch {
      setError(true);
    }
  }, [movieId]);

  useEffect(() => {
    load();
  }, [load]);

  // --- watch-progress sync (same watchProgress/{uid}/items/{movieId} as Android) ---
  const progressRef = useCallback(
    () => (uid ? doc(db, "watchProgress", uid, "items", movieId) : null),
    [uid, movieId],
  );

  const saveProgress = useCallback(async () => {
    const v = videoRef.current;
    const ref = progressRef();
    if (!v || !ref || !v.duration || Number.isNaN(v.duration)) return;
    const progress = Math.min(1, Math.max(0, v.currentTime / v.duration));
    try {
      await setDoc(
        ref,
        {
          movieId,
          positionMs: Math.floor(v.currentTime * 1000),
          durationMs: Math.floor(v.duration * 1000),
          progress,
          completed: progress >= COMPLETE_THRESHOLD,
          updatedAt: serverTimestamp(),
        },
        { merge: true },
      );
    } catch {
      /* best-effort */
    }
  }, [progressRef, movieId]);

  const applyResume = useCallback(async () => {
    const v = videoRef.current;
    const ref = progressRef();
    if (!v || !ref || appliedResume.current) return;
    appliedResume.current = true;
    try {
      const snap = await getDoc(ref);
      if (!snap.exists()) return;
      const d = snap.data();
      if (d.completed) return;
      const posMs = Number(d.positionMs ?? 0);
      if (posMs > 0 && v.duration && posMs / 1000 < v.duration * 0.97) {
        v.currentTime = posMs / 1000;
        setResumeMs(posMs);
      }
    } catch {
      /* ignore */
    }
  }, [progressRef]);

  useEffect(() => {
    if (!uid) return;
    const id = setInterval(() => {
      if (!videoRef.current?.paused) saveProgress();
    }, 10000);
    const onHide = () => saveProgress();
    document.addEventListener("visibilitychange", onHide);
    window.addEventListener("pagehide", onHide);
    return () => {
      clearInterval(id);
      document.removeEventListener("visibilitychange", onHide);
      window.removeEventListener("pagehide", onHide);
    };
  }, [uid, saveProgress]);

  useEffect(() => {
    if (resumeMs == null) return;
    const t = setTimeout(() => setResumeMs(null), 6000);
    return () => clearTimeout(t);
  }, [resumeMs]);

  // --- controls ---
  const revealControls = useCallback(() => {
    setControlsVisible(true);
    if (hideTimer.current) clearTimeout(hideTimer.current);
    hideTimer.current = setTimeout(() => {
      if (!videoRef.current?.paused) setControlsVisible(false);
    }, 3000);
  }, []);

  const togglePlay = useCallback(() => {
    const v = videoRef.current;
    if (!v) return;
    if (v.paused) v.play();
    else v.pause();
  }, []);

  const toggleMute = useCallback(() => {
    const v = videoRef.current;
    if (v) v.muted = !v.muted;
  }, []);

  const changeVolume = useCallback((value: number) => {
    const v = videoRef.current;
    if (!v) return;
    v.volume = Math.min(1, Math.max(0, value));
    v.muted = v.volume === 0;
  }, []);

  const seek = useCallback((value: number) => {
    const v = videoRef.current;
    if (v) v.currentTime = value;
  }, []);

  const startOver = useCallback(() => {
    if (videoRef.current) videoRef.current.currentTime = 0;
    setResumeMs(null);
  }, []);

  const toggleFullscreen = useCallback(() => {
    if (document.fullscreenElement) document.exitFullscreen();
    else containerRef.current?.requestFullscreen();
  }, []);

  const togglePip = useCallback(async () => {
    const v = videoRef.current;
    if (!v) return;
    try {
      if (document.pictureInPictureElement) await document.exitPictureInPicture();
      else if (document.pictureInPictureEnabled) await v.requestPictureInPicture();
    } catch {
      /* ignore */
    }
  }, []);

  // Keyboard shortcuts — only in full mode.
  useEffect(() => {
    if (minimized) return;
    const onKey = (e: KeyboardEvent) => {
      const v = videoRef.current;
      if (!v) return;
      switch (e.key) {
        case " ":
        case "k":
          e.preventDefault();
          togglePlay();
          break;
        case "ArrowLeft":
          e.preventDefault();
          v.currentTime = Math.max(0, v.currentTime - 5);
          break;
        case "ArrowRight":
          e.preventDefault();
          v.currentTime = Math.min(v.duration || Infinity, v.currentTime + 5);
          break;
        case "ArrowUp":
          e.preventDefault();
          changeVolume(v.volume + 0.1);
          break;
        case "ArrowDown":
          e.preventDefault();
          changeVolume(v.volume - 0.1);
          break;
        case "f":
          toggleFullscreen();
          break;
        case "m":
          toggleMute();
          break;
        default:
          return;
      }
      revealControls();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [minimized, togglePlay, toggleMute, toggleFullscreen, changeVolume, revealControls]);

  useEffect(() => {
    const onFsChange = () => setFullscreen(Boolean(document.fullscreenElement));
    document.addEventListener("fullscreenchange", onFsChange);
    return () => document.removeEventListener("fullscreenchange", onFsChange);
  }, []);

  const video = (
    <>
      {src ? (
        // eslint-disable-next-line jsx-a11y/media-has-caption
        <video
          ref={videoRef}
          src={src}
          poster={active.poster || undefined}
          autoPlay
          className="h-full w-full object-contain"
          onClick={minimized ? () => router.push(`/watch/${movieId}`) : togglePlay}
          onPlay={() => setPlaying(true)}
          onPause={() => {
            setPlaying(false);
            saveProgress();
          }}
          onEnded={() => {
            setPlaying(false);
            saveProgress();
          }}
          onWaiting={() => setBuffering(true)}
          onPlaying={() => setBuffering(false)}
          onTimeUpdate={(e) => setCurrent(e.currentTarget.currentTime)}
          onLoadedMetadata={(e) => {
            setDuration(e.currentTarget.duration || 0);
            applyResume();
          }}
          onDurationChange={(e) => setDuration(e.currentTarget.duration || 0)}
          onVolumeChange={(e) => {
            setVolume(e.currentTarget.volume);
            setMuted(e.currentTarget.muted);
          }}
        />
      ) : error ? (
        <div className="flex h-full items-center justify-center">
          <button
            type="button"
            onClick={load}
            className="h-9 rounded-lg bg-brand-500 px-4 text-sm font-semibold text-white hover:bg-brand-600"
          >
            {dict.retry}
          </button>
        </div>
      ) : (
        <div className="flex h-full items-center justify-center">
          <Loader2 className="h-8 w-8 animate-spin text-white" aria-label={dict.loading} />
        </div>
      )}
      {src && buffering && (
        <Loader2 className="pointer-events-none absolute h-10 w-10 animate-spin text-white" aria-hidden />
      )}
    </>
  );

  // ---- Mini-player ----
  if (minimized) {
    return (
      <div className="fixed bottom-4 right-4 z-50 aspect-video w-64 overflow-hidden rounded-xl border border-surface-border bg-black shadow-2xl md:w-80">
        {video}
        <div className="absolute inset-0 flex items-center justify-center gap-3 bg-black/30 opacity-0 transition-opacity hover:opacity-100">
          <IconBtn label={playing ? dict.pause : dict.play} onClick={togglePlay}>
            {playing ? <Pause className="h-5 w-5" /> : <Play className="h-5 w-5 fill-current" />}
          </IconBtn>
          <IconBtn label={dict.expand} onClick={() => router.push(`/watch/${movieId}`)}>
            <Maximize className="h-5 w-5" />
          </IconBtn>
        </div>
        <button
          type="button"
          aria-label={dict.close}
          onClick={onClose}
          className="absolute right-1 top-1 rounded-full bg-black/60 p-1 text-white hover:bg-black/80"
        >
          <X className="h-4 w-4" />
        </button>
      </div>
    );
  }

  // ---- Full player ----
  return (
    <div
      ref={containerRef}
      className="fixed inset-0 z-50 flex items-center justify-center bg-black"
      onMouseMove={revealControls}
      onTouchStart={revealControls}
    >
      {video}

      <button
        type="button"
        aria-label={dict.close}
        onClick={() => router.back()}
        className={cn(
          "absolute left-4 top-4 z-10 inline-flex h-10 items-center gap-2 rounded-lg bg-black/50 px-3 text-sm font-medium text-white backdrop-blur transition-opacity hover:bg-black/70",
          controlsVisible ? "opacity-100" : "opacity-0",
        )}
      >
        <ArrowLeft className="h-4 w-4" aria-hidden />
        <span className="max-w-[50vw] truncate">{active.title}</span>
      </button>

      {resumeMs != null && (
        <div className="absolute left-1/2 top-6 z-20 flex -translate-x-1/2 items-center gap-2 rounded-full bg-black/70 py-1.5 pl-4 pr-2 text-sm text-white backdrop-blur">
          <span>
            {dict.resumeFrom} {formatTime(resumeMs / 1000)}
          </span>
          <button
            type="button"
            onClick={startOver}
            className="rounded-full px-3 py-1 font-semibold text-brand-400 hover:bg-white/10"
          >
            {dict.startOver}
          </button>
        </div>
      )}

      {src && (
        <div
          className={cn(
            "absolute inset-x-0 bottom-0 z-10 bg-gradient-to-t from-black/80 to-transparent px-4 pb-4 pt-16 transition-opacity duration-300",
            controlsVisible ? "opacity-100" : "opacity-0",
          )}
        >
          <input
            type="range"
            min={0}
            max={duration || 0}
            step={0.1}
            value={Math.min(current, duration || 0)}
            onChange={(e) => seek(Number(e.target.value))}
            aria-label={dict.seek}
            className="h-1 w-full cursor-pointer accent-brand-500"
          />
          <div className="mt-2 flex items-center gap-3 text-white">
            <button type="button" onClick={togglePlay} aria-label={playing ? dict.pause : dict.play}>
              {playing ? <Pause className="h-6 w-6" /> : <Play className="h-6 w-6 fill-current" />}
            </button>
            <button type="button" onClick={toggleMute} aria-label={muted ? dict.unmute : dict.mute}>
              {muted || volume === 0 ? <VolumeX className="h-5 w-5" /> : <Volume2 className="h-5 w-5" />}
            </button>
            <input
              type="range"
              min={0}
              max={1}
              step={0.05}
              value={muted ? 0 : volume}
              onChange={(e) => changeVolume(Number(e.target.value))}
              aria-label={dict.volume}
              className="h-1 w-24 cursor-pointer accent-brand-500"
            />
            <span className="text-xs tabular-nums text-ink-muted">
              {formatTime(current)} / {formatTime(duration)}
            </span>
            <div className="flex-1" />
            <button type="button" onClick={togglePip} aria-label={dict.pip}>
              <PictureInPicture2 className="h-5 w-5" />
            </button>
            <button type="button" onClick={toggleFullscreen} aria-label={dict.fullscreen}>
              {fullscreen ? <Minimize className="h-5 w-5" /> : <Maximize className="h-5 w-5" />}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

function IconBtn({
  label,
  onClick,
  children,
}: {
  label: string;
  onClick: () => void;
  children: ReactNode;
}) {
  return (
    <button
      type="button"
      aria-label={label}
      onClick={onClick}
      className="rounded-full bg-black/50 p-2 text-white hover:bg-black/70"
    >
      {children}
    </button>
  );
}
