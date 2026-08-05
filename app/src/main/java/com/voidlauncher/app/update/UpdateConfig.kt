package com.voidlauncher.app.update

object UpdateConfig {
    /** GitHub "owner/repo" that hosts Releases with a VoidLauncher.apk asset. */
    const val REPO = "slimeryt/void-launcher"
    const val APK_ASSET_NAME = "VoidLauncher.apk"
    const val LATEST_API = "https://api.github.com/repos/$REPO/releases/latest"
    const val USER_AGENT = "VoidLauncher-Updater"
}
