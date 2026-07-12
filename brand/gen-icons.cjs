/* Generates every app-icon size from brand/filmatube-logo.png (the exact logo). */
const path = require("path");
const sharp = require("C:/AppBase/FilmatubeBase/WebApp/Filmatube/node_modules/sharp");

const SRC = "C:/AppBase/FilmatubeBase/brand/filmatube-logo.png";
const RES = "C:/AppBase/FilmatubeBase/Android/Filmatube/app/src/main/res";
const AND = "C:/AppBase/FilmatubeBase/Android/Filmatube/app/src/main";
const WEB = "C:/AppBase/FilmatubeBase/WebApp/Filmatube";

const transparent = { r: 0, g: 0, b: 0, alpha: 0 };

async function png(size, out) {
  await sharp(SRC).resize(size, size, { fit: "contain", background: transparent }).png().toFile(out);
}
async function webp(size, out) {
  await sharp(SRC).resize(size, size, { fit: "contain", background: transparent }).webp({ quality: 100 }).toFile(out);
}
async function roundWebp(size, out) {
  const circle = Buffer.from(
    `<svg width="${size}" height="${size}"><circle cx="${size / 2}" cy="${size / 2}" r="${size / 2}" fill="#fff"/></svg>`,
  );
  await sharp(SRC)
    .resize(size, size, { fit: "contain", background: transparent })
    .composite([{ input: circle, blend: "dest-in" }])
    .webp({ quality: 100 })
    .toFile(out);
}

// Adaptive foreground fills the full 108dp canvas (transparent margins reveal the background).
const FG = { mdpi: 108, hdpi: 162, xhdpi: 216, xxhdpi: 324, xxxhdpi: 432 };
// Legacy square/round launcher raster sizes.
const LG = { mdpi: 48, hdpi: 72, xhdpi: 96, xxhdpi: 144, xxxhdpi: 192 };

(async () => {
  // Web
  await png(512, path.join(WEB, "app/icon.png")); // browser tab (Next auto-favicon)
  await png(180, path.join(WEB, "app/apple-icon.png"));
  await png(512, path.join(WEB, "public/logo.png")); // in-app Wordmark
  await png(192, path.join(WEB, "public/icon-192.png"));
  await png(512, path.join(WEB, "public/icon-512.png"));

  // Android Play Store listing icon
  await png(512, path.join(AND, "ic_launcher-playstore.png"));

  // Android adaptive foreground + legacy raster per density
  for (const [d, size] of Object.entries(FG)) {
    await webp(size, path.join(RES, `mipmap-${d}/ic_launcher_foreground.webp`));
  }
  for (const [d, size] of Object.entries(LG)) {
    await webp(size, path.join(RES, `mipmap-${d}/ic_launcher.webp`));
    await roundWebp(size, path.join(RES, `mipmap-${d}/ic_launcher_round.webp`));
  }

  console.log("icons generated");
})().catch((e) => {
  console.error(e);
  process.exit(1);
});
