package com.voidlauncher.app.update

object UpdateConfig {
    /** GitHub "owner/repo" that hosts Releases with a Polar.apk asset. */
    const val REPO = "slimeryt/void-launcher"
    const val APK_ASSET_NAME = "Polar.apk"
    /** Legacy asset name — still published so older builds can update. */
    const val LEGACY_APK_ASSET_NAME = "VoidLauncher.apk"

    /** Prefer HTML latest redirect — not rate-limited like api.github.com. */
    const val LATEST_HTML = "https://github.com/$REPO/releases/latest"
    const val LATEST_API = "https://api.github.com/repos/$REPO/releases/latest"
    /** List endpoint for beta / developer channel selection. */
    const val RELEASES_API = "https://api.github.com/repos/$REPO/releases?per_page=40"

    fun downloadUrl(tag: String): String =
        "https://github.com/$REPO/releases/download/$tag/$APK_ASSET_NAME"

    fun legacyDownloadUrl(tag: String): String =
        "https://github.com/$REPO/releases/download/$tag/$LEGACY_APK_ASSET_NAME"

    /** GitHub requires a descriptive UA; bare names often get 403. */
    const val USER_AGENT =
        "Polar-Updater/1.0"
}
