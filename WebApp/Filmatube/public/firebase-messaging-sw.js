/* Firebase Cloud Messaging background handler.
 * Config is passed as query params at registration time (the values are public
 * NEXT_PUBLIC_* keys). Background pushes are shown here; foreground ones are
 * handled by onMessage in PushRegistration. */
importScripts("https://www.gstatic.com/firebasejs/11.3.0/firebase-app-compat.js");
importScripts("https://www.gstatic.com/firebasejs/11.3.0/firebase-messaging-compat.js");

const params = new URL(self.location).searchParams;
firebase.initializeApp({
  apiKey: params.get("apiKey"),
  authDomain: params.get("authDomain"),
  projectId: params.get("projectId"),
  messagingSenderId: params.get("messagingSenderId"),
  appId: params.get("appId"),
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
  const notification = payload.notification || {};
  const data = payload.data || {};
  self.registration.showNotification(notification.title || "Filmatube", {
    body: notification.body || "",
    icon: "/icon-192.png",
    data,
  });
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  const data = event.notification.data || {};
  const url = data.route || (data.movieId ? `/movie/${data.movieId}` : "/notifications");
  event.waitUntil(clients.openWindow(url));
});
