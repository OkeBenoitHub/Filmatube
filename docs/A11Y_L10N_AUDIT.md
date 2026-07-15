# Accessibility & Localization Audit — Android (Day 118)

## Localization (FR sweep)
- **String parity: complete.** `values/strings.xml` = 301 keys, `values-fr/strings.xml` = 300.
  The only EN key without an FR entry is `google_web_client_id` — a config value (OAuth client id),
  not user-facing text, so it correctly has no translation.
- **No hardcoded UI text** in Compose: all copy is `stringResource(...)`; both locales updated together.
- Web mirrors this via the type-enforced `Dictionary` (FR must structurally match EN at compile time).
- Per-app language switching works (AppCompat `setApplicationLocales` + `locales_config`).

## Accessibility
### Touch targets (fixed)
- **Rating stars** were 32dp `clickable` icons (below the 48dp minimum). Reworked as `IconButton`s
  (48dp touch target) with a 30dp star glyph — same look, accessible hit area.
- Other interactive controls already meet the minimum: `IconButton`s (back, mute, bell, overflow) are
  48dp; reaction/segment chips use `Surface` + `padding(h=12,v=6)`; primary/secondary buttons are 48–52dp.

### Content descriptions (verified)
- Icon buttons and meaningful icons carry `contentDescription` via `stringResource` (back, play,
  rewind/forward, lock, PiP, resize, mute, like, rate N stars, reactions, notifications, etc.).
- Decorative icons/images pass `contentDescription = null` (poster tiles have the title elsewhere;
  gradient/glow layers are non-semantic).
- `UserAvatar` uses the person's name as its description.

### Contrast (green dark theme)
- Body text `onSurface #DEE4DD` / muted `onSurfaceVariant #BFC9BF` on `surface #0E1512` clear 4.5:1.
- Primary green `#5DD08A` on dark surface and `onPrimary #00391E` on the green button exceed AA.
- Gold accent `#E6C463` used for small labels on dark — passes AA for the sizes used.
- The earlier **black-default-text bug is fixed** (root `Surface` sets `LocalContentColor`), so no
  invisible/low-contrast text remains from unstyled `Text`.

### TalkBack notes
- Screens use standard Material 3 components with semantics; headings use `titleLarge`/`headline*`.
- Manual TalkBack pass recommended on: player controls (custom overlay), the reactions/rating row,
  and the feed — confirm focus order and that emoji reactions announce their label, not the glyph.

## Status
Android `assembleDebug` green. FR parity complete; rating-star touch targets fixed; contrast verified
against the palette. Remaining a11y work is a manual on-device TalkBack pass (checklist above).
