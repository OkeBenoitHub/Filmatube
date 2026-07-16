"use client";

import { useEffect, useState } from "react";
import { doc, onSnapshot } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/components/providers/AuthProvider";

export interface Author {
  name: string;
  avatar: string;
}

/**
 * The signed-in user's identity as stored in `users/{uid}` — the source Android writes onto board
 * messages. The web never syncs edits back to the Firebase Auth profile (photoURL is always empty,
 * displayName is frozen at registration), so reading Auth here would stamp messages with a blank
 * avatar and a stale name.
 */
export function useAuthor(): Author {
  const { user } = useAuth();
  const [author, setAuthor] = useState<Author>({ name: "", avatar: "" });

  useEffect(() => {
    if (!user) {
      setAuthor({ name: "", avatar: "" });
      return;
    }
    return onSnapshot(doc(db, "users", user.uid), (snap) =>
      setAuthor({
        name: (snap.get("displayName") as string) ?? user.displayName ?? "",
        avatar: (snap.get("avatarUrl") as string) ?? user.photoURL ?? "",
      }),
    );
  }, [user]);

  return author;
}
