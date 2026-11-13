# Reel Counter

## Get the APK without Android Studio (easiest)
1. Make a free account at github.com, click **New repository**, name it `ReelCounter` (private is fine).
2. Click **uploading an existing file**, drag in EVERYTHING from this folder (including the hidden `.github` folder), **Commit**.
3. Open the **Actions** tab > **Build APK** > wait ~3-5 min for the green tick > open the run > download **ReelCounter-APK** (a zip) > unzip to get `app-debug.apk`.
   (If it didn't start by itself: Actions > Build APK > Run workflow.)

## Build with Android Studio
File > Open > pick this folder > wait for Gradle sync (if asked, use Gradle 8.9 / JDK 17) > Build > Build APK(s). Or press Run with your phone plugged in (USB debugging on).

## Install on the phone
Copy `app-debug.apk` to the phone, tap it, allow "install unknown apps" for your file manager/browser when asked.

## Enable counting
1. Open Reel Counter, follow the onboarding buttons.
2. Settings > Accessibility > Installed apps > Reel Counter > turn ON.
3. Android 13+ greys it out for sideloaded apps? Go to Settings > Apps > Reel Counter > (⋮ menu) > **Allow restricted settings**, then retry.
4. Settings > Apps > Reel Counter > Battery > Unrestricted.

## Add the widget
Long-press the home screen > Widgets > Reel Counter > drag it out, resize as you like.

## Your own brain images
Delete `app/src/main/res/drawable/brain_happy.xml` and `brain_tired.xml`, drop in `brain_happy.png` and `brain_tired.png` (exact names, lowercase), rebuild.

## Tuning detection
Instagram's internal view IDs change. Edit `IG_REEL_IDS` in `CounterService.kt`. Hidden debug mode: tap the "v1.0" text on the main screen 7 times; the pill then shows the scroll event class and view ID.
