# PixelVeil

Turns screen pixels off (black) in patterns and shapes, up to 95% (90% preset).
Made for AMOLED phones such as the Galaxy S23 FE.

## Build the APK with GitHub Actions
1. Create a GitHub repo and upload everything in this folder, keeping the paths
   (including `.github/workflows/build.yml`).
2. Open the Actions tab, run "Build APK" (it also runs on every push).
3. Download the `PixelVeil-apk` artifact, unzip it, install `app-debug.apk`.

## First run on the phone
- Overlay mode: allow "Display over other apps" when asked.
- Accessibility mode (true black + covers status/nav bars): Settings > Accessibility > Installed apps > PixelVeil.
  If greyed out: Settings > Apps > PixelVeil > ⋮ > Allow restricted settings.
