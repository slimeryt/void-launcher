package com.voidlauncher.app.update

object UpdateConfig {
    /** GitHub "owner/repo" that hosts Releases with a VoidLauncher.apk asset. */
    const val REPO = "slimeryt/void-launcher"
    const val APK_ASSET_NAME = "VoidLauncher.apk"

    /** Prefer HTML latest redirect — not rate-limited like api.github.com. */
    const val LATEST_HTML = "https://github.com/$REPO/releases/latest"
    const val LATEST_API = "https://api.github.com/repos/$REPO/releases/latest"

    fun downloadUrl(tag: String): String =
        "https://github.com/$REPO/releases/download/$tag/$APK_ASSET_NAME"

    /** GitHub requires a descriptive UA; bare names often get 403. */
    const val USER_AGENT =
        "VoidLauncher-Updater/1.0 (+https://github.com/slimeryt/void-launcher)"
}
