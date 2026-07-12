"use client";

import { useEffect } from "react";
import { doc, serverTimestamp, setDoc } from "firebase/firestore";
import { db, firebaseApp } from "@/lib/firebase";
import { useAuth } from "@/components/providers/AuthProvider";

/**
 * Registers the browser for FCM web push once a user is signed in:
 * requests permission, gets a token, and stores it under users/{uid}/fcmTokens.
 * No-op unless NEXT_PUBLIC_FIREBASE_VAPID_KEY is set (Web Push certificate).
 */
export function PushRegistration() {
  const { user } = useAuth();

  useEffect(() => {
    const vapidKey = process.env.NEXT_PUBLIC_FIREBASE_VAPID_KEY;
    if (!user || !vapidKey) return;
    if (typeof window === "undefined" || typeof Notification === "undefined") return;

    let cancelled = false;
    (async () => {
      const { getMessaging, getToken, isSupported, onMessage } = await import("firebase/messaging");
      if (!(await isSupported())) return;

      const permission = await Notification.requestPermission();
      if (permission !== "granted" || cancelled) return;

      const cfg = new URLSearchParams({
        apiKey: process.env.NEXT_PUBLIC_FIREBASE_API_KEY ?? "",
        authDomain: process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN ?? "",
        projectId: process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID ?? "",
        messagingSenderId: process.env.NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID ?? "",
        appId: process.env.NEXT_PUBLIC_FIREBASE_APP_ID ?? "",
      });
      const registration = await navigator.serviceWorker.register(`/firebase-messaging-sw.js?${cfg.toString()}`);

      const messaging = getMessaging(firebaseApp);
      const token = await getToken(messaging, { vapidKey, serviceWorkerRegistration: registration });
      if (token && !cancelled) {
        await setDoc(doc(db, "users", user.uid, "fcmTokens", token), {
          token,
          platform: "web",
          updatedAt: serverTimestamp(),
        });
      }
      // Foreground messages: the in-app notification center updates live, so just no-op.
      onMessage(messaging, () => {});
    })().catch(() => {
      /* push unavailable — silently ignore */
    });

    return () => {
      cancelled = true;
    };
  }, [user]);

  return null;
}
