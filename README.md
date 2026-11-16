# Lamnian — native Android app (Kotlin + Jetpack Compose)

Same Firebase project as the website (`lamnian-ai`): same accounts, posts, comments, likes, profiles and reports.
Built to an APK by GitHub Actions. Package name: **com.lamnian.app**

## 1. Firebase
The app is signed with the original `app/lamnian.keystore` (alias `lamnian`, password `lamnian123`). Register its fingerprints in Firebase (Project settings → Your apps → `com.lamnian.app` → Add fingerprint):
- SHA-1: `B3:0C:3A:84:16:64:DF:D9:EB:DE:63:8B:63:01:0D:3B:5A:E3:71:45`
- SHA-256: `7C:2E:CC:DD:E9:C6:A2:F2:6C:9F:10:D9:47:9F:A9:8E:79:79:89:3C:76:A0:1F:04:71:65:13:59:35:80:39:75`

The included `app/google-services.json` already has this key registered, so Google sign-in is ready.
Authentication → Sign-in method: **Google** and **Email/Password** must be enabled. Publish the included `firestore.rules` in Firebase → Firestore → Rules.

## 2. Build with GitHub Actions
1. Create a **private** GitHub repository and upload everything in this folder, including the hidden `.github` folder and `app/google-services.json`.
2. Open the **Actions** tab → *Build Lamnian APK* → Run workflow (it also runs on every push).
3. When it finishes, download the **Lamnian-APK** artifact. Unzip it and install `app-release.apk` on a phone.

## 3. Settings you may want to change — `app/src/main/java/com/lamnian/app/Config.kt`
- `API_BASE`: `"https://lamnian.vercel.app"`. Used for the share link and the admin link in Settings (admin account only).
- `WEATHER_VIA_API`: `false` = weather straight from Open-Meteo's free API (no key). Set `true` only with a paid Open-Meteo key on Vercel.
- `LAT` / `LON`: village coordinates (weather, air quality, prayer times).

## What is inside
Home feed (news, photos, events, notices, search, Top), post/comment/like/report/edit/delete, Facebook-style profiles
(posts, likes, saved), one Settings gear (profile, theme, password email, sign out, delete posts, delete account),
weather (hourly, 7-day, tips, sun, prayer times, air quality), directory with emergency numbers, Ask AI placeholder,
splash screen, dark mode, offline reading (Firestore cache).

## Good to know
- The `lamnian.keystore` and its passwords are in the project so every build has the same SHA-1. Keep the repo **private**.
  Before publishing on Google Play, create your own private upload key and keep it out of GitHub.
- A profile shows that person's posts from the latest 60 of each type.
- Phone notifications and an Urdu version are not included yet.
