# Void Launcher

A minimal glass-aesthetic Android home screen launcher inspired by Void Launcher.

## Features

- **Home screen** with live clock over your system wallpaper
- **Live liquid glass** with wallpaper blur + lens refraction / chromatic CA
- **Glass dock** with favorite apps
- **Swipe-up app drawer** (swipe down to close) with search
- **Long-press home** → edit mode → **Settings** (solid black activity)
- **Settings** also has GitHub Releases auto-updater
- Registers as a system **HOME** launcher
- App icon opens the **launcher home** (not Settings)

## Updates (OTA)

Repo: [`slimeryt/void-launcher`](https://github.com/slimeryt/void-launcher)

In the app: **Void (Settings) → Updates → Check / Download / Install**.

Publish a build:

```bash
git tag v0.1.0
git push origin v0.1.0
```

Or run the **Release APK** GitHub Action manually. It uploads `VoidLauncher.apk` to a Release; the app installs that asset.

Release body should include `versionCode: N` (the workflow adds this automatically).

## Signing / OTA

All APKs are signed with `signing/void-ota.jks` (local + CI) so Updates can install over previous builds.

If Android says the package conflicts, uninstall the old Void once, then install the new APK — after that, Settings → Updates works.

## Gestures

| Action | Result |
|--------|--------|
| Swipe up on home | Open app drawer |
| Swipe down in apps | Close drawer |
| Long-press home | Edit mode → Settings |
| Long-press icon | Favorite / hide / info |
| Open Void app icon | Launcher home |
| Long-press home → Settings | Solid-black settings + updater |

## Stack

Kotlin · Jetpack Compose · Material 3 · DataStore · minSdk 26
