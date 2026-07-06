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
  auth: {
    loginTitle: "Welcome back",
    loginSubtitle: "Sign in to keep watching",
    registerTitle: "Create your account",
    registerSubtitle: "Join the movie community",
    name: "Name",
    email: "Email",
    password: "Password",
    confirmPassword: "Confirm password",
    signIn: "Sign in",
    createAccount: "Create account",
    continueGoogle: "Continue with Google",
    orDivider: "or",
    noAccount: "Don't have an account?",
    haveAccount: "Already have an account?",
    signUp: "Sign up",
    forgotPassword: "Forgot password?",
    forgotTitle: "Reset password",
    forgotSubtitle: "Enter your email and we'll send you a reset link.",
    forgotSend: "Send reset link",
    forgotSentTitle: "Check your email",
    forgotSentMessage: "If an account exists for that email, a reset link is on its way.",
    backToLogin: "Back to sign in",
    errors: {
      emailRequired: "Enter your email",
      emailInvalid: "Enter a valid email",
      passwordRequired: "Enter your password",
      passwordShort: "Password must be at least 6 characters",
      nameRequired: "Enter your name",
      passwordMismatch: "Passwords don't match",
      invalidCredentials: "Incorrect email or password",
      emailInUse: "That email is already registered",
      network: "No internet connection",
      tooMany: "Too many attempts. Try again later",
      generic: "Something went wrong. Please try again",
    },
  },
  landing: {
    badge: "Streaming, made social",
    title: "Watch. Share. Discuss.",
    tagline:
      "Stream movies, follow people who share your taste, and talk about it all in real time.",
    ctaPrimary: "Get early access",
    ctaHint: "Android app and web player in active development.",
    storeBadge: "Soon on Google Play",
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
  auth: {
    loginTitle: "Bon retour",
    loginSubtitle: "Connectez-vous pour continuer à regarder",
    registerTitle: "Créez votre compte",
    registerSubtitle: "Rejoignez la communauté cinéma",
    name: "Nom",
    email: "E-mail",
    password: "Mot de passe",
    confirmPassword: "Confirmer le mot de passe",
    signIn: "Se connecter",
    createAccount: "Créer un compte",
    continueGoogle: "Continuer avec Google",
    orDivider: "ou",
    noAccount: "Pas encore de compte ?",
    haveAccount: "Vous avez déjà un compte ?",
    signUp: "S'inscrire",
    forgotPassword: "Mot de passe oublié ?",
    forgotTitle: "Réinitialiser le mot de passe",
    forgotSubtitle: "Saisissez votre e-mail et nous vous enverrons un lien de réinitialisation.",
    forgotSend: "Envoyer le lien",
    forgotSentTitle: "Vérifiez vos e-mails",
    forgotSentMessage: "Si un compte existe pour cet e-mail, un lien de réinitialisation est en route.",
    backToLogin: "Retour à la connexion",
    errors: {
      emailRequired: "Saisissez votre e-mail",
      emailInvalid: "Saisissez un e-mail valide",
      passwordRequired: "Saisissez votre mot de passe",
      passwordShort: "Le mot de passe doit contenir au moins 6 caractères",
      nameRequired: "Saisissez votre nom",
      passwordMismatch: "Les mots de passe ne correspondent pas",
      invalidCredentials: "E-mail ou mot de passe incorrect",
      emailInUse: "Cet e-mail est déjà enregistré",
      network: "Pas de connexion Internet",
      tooMany: "Trop de tentatives. Réessayez plus tard",
      generic: "Une erreur s'est produite. Veuillez réessayer",
    },
  },
  landing: {
    badge: "Le streaming, en version sociale",
    title: "Regarder. Partager. Discuter.",
    tagline:
      "Regardez des films, suivez des gens qui partagent vos goûts et discutez-en en temps réel.",
    ctaPrimary: "Accès anticipé",
    ctaHint: "Application Android et lecteur web en cours de développement.",
    storeBadge: "Bientôt sur Google Play",
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
