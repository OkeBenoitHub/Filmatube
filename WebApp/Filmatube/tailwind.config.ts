import type { Config } from "tailwindcss";

/**
 * Filmatube web theme — green brand + dark surface tokens, mirroring the Android
 * Material 3 forced-dark palette so both platforms look identical.
 */
const config: Config = {
  content: [
    "./app/**/*.{ts,tsx,mdx}",
    "./components/**/*.{ts,tsx,mdx}",
    "./lib/**/*.{ts,tsx}",
  ],
  darkMode: "class",
  theme: {
    extend: {
      colors: {
        brand: {
          50: "#E8F8EF",
          100: "#C8EFD9",
          200: "#9FE3BD",
          300: "#7FE0A4",
          400: "#5DD08A", // Android primary
          500: "#2E9E5B", // brand base
          600: "#25814A",
          700: "#176E3C", // deep
          800: "#154E2D",
          900: "#0F3A22",
          950: "#082015",
        },
        surface: {
          DEFAULT: "#0E1512", // background
          card: "#1A211D", // surfaceContainer
          hover: "#252C27", // surfaceContainerHigh
          border: "#2F3632", // outline/surfaceContainerHighest
        },
        ink: {
          DEFAULT: "#DEE4DD", // onSurface
          muted: "#BFC9BF", // onSurfaceVariant
          faint: "#899389", // outline
        },
        gold: "#E6C463", // ratings / premieres accent
      },
      fontFamily: {
        sans: ["Inter", "system-ui", "-apple-system", "sans-serif"],
      },
    },
  },
  plugins: [],
};

export default config;
