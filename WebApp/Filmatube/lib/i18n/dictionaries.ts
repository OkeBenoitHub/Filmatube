import type { Locale } from "./config";

/**
 * UI copy for the whole web app — the single source of truth for user-facing text.
 *
 * RULE: never hardcode UI strings in components/pages. Add them here (EN) and in `fr`
 * below, then read them via `getDict()` (server) or `useI18n()` (client). This mirrors
 * the Android convention of `values/strings.xml` + `values-fr/strings.xml`.
 */
const en = {
  common: {
    appName: "Filmatube",
    comingSoon: "Coming soon",
    signIn: "Sign in",
    language: "Language",
  },
  landing: {
    badge: "Streaming, made social",
    title: "Watch. Share. Discuss.",
    tagline:
      "Stream movies, follow people who share your taste, and talk about it all in real time.",
    ctaPrimary: "Get early access",
    ctaHint: "Android app and web player in active development.",
    featuresKicker: "Why Filmatube",
    featuresTitle: "Everything a movie night needs",
    features: {
      watch: {
        title: "Watch anywhere",
        desc: "Stream in HD or download for offline — your movies follow you across devices.",
      },
      social: {
        title: "Find your taste twins",
        desc: "Follow people who love what you love and see what they're watching.",
      },
      theater: {
        title: "Online movie theater",
        desc: "Join scheduled showtimes and live premieres, perfectly in sync with everyone.",
      },
      boards: {
        title: "Talk in real time",
        desc: "Discussion boards, spoiler tags and watch parties with your people.",
      },
    },
    footerTagline: "Movies with your people.",
    footerRights: "All rights reserved.",
  },
  watch: {
    title: "The web player is coming",
    subtitle: "Stream in HD with subtitles and audio tracks — right in your browser.",
  },
  admin: {
    title: "Filmatube Studio",
    subtitle: "The admin CMS — manage movies, users, requests, showtimes and more.",
  },
};

export type Dictionary = typeof en;

const fr: Dictionary = {
  common: {
    appName: "Filmatube",
    comingSoon: "Bientôt disponible",
    signIn: "Se connecter",
    language: "Langue",
  },
  landing: {
    badge: "Le streaming, en version sociale",
    title: "Regarder. Partager. Discuter.",
    tagline:
      "Regardez des films, suivez des gens qui partagent vos goûts et discutez-en en temps réel.",
    ctaPrimary: "Accès anticipé",
    ctaHint: "Application Android et lecteur web en cours de développement.",
    featuresKicker: "Pourquoi Filmatube",
    featuresTitle: "Tout ce qu'il faut pour une soirée cinéma",
    features: {
      watch: {
        title: "Regardez partout",
        desc: "Streaming HD ou téléchargement hors ligne — vos films vous suivent sur tous vos appareils.",
      },
      social: {
        title: "Trouvez vos jumeaux de goût",
        desc: "Suivez des gens qui aiment ce que vous aimez et découvrez ce qu'ils regardent.",
      },
      theater: {
        title: "Cinéma en ligne",
        desc: "Rejoignez des séances programmées et des premières en direct, parfaitement synchronisées.",
      },
      boards: {
        title: "Discutez en temps réel",
        desc: "Forums de discussion, balises spoiler et soirées cinéma avec vos proches.",
      },
    },
    footerTagline: "Des films avec vos proches.",
    footerRights: "Tous droits réservés.",
  },
  watch: {
    title: "Le lecteur web arrive",
    subtitle: "Streaming HD avec sous-titres et pistes audio — directement dans votre navigateur.",
  },
  admin: {
    title: "Filmatube Studio",
    subtitle: "Le CMS d'administration — gérez films, utilisateurs, demandes, séances et plus.",
  },
};

export const dictionaries: Record<Locale, Dictionary> = { en, fr };
