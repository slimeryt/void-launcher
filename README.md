# Polar

A minimal glass-aesthetic Android home screen launcher.

## Updates (OTA)

Repo: [`slimeryt/void-launcher`](https://github.com/slimeryt/void-launcher)

In the app: **Polar (Settings) → Updates → Check / Download / Install**.

Releases publish **Polar.apk** (and a legacy **VoidLauncher.apk** copy for older builds).

### “Invalid package” / package conflicts

That almost always means the installed build was signed with a different key than the GitHub APK.

1. Uninstall Polar/Void once  
2. Install **Polar.apk** from the latest Release  
3. After that, Settings → Updates works with the shared OTA key

Or run the **Release APK** GitHub Action manually.

## Home

| Action | Result |
|---|---|
| Open Polar app icon | Launcher home |
