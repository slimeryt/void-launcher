package com.voidlauncher.app.update

/**
 * Software update enrollment — mirrors Apple's "Beta Updates" picker.
 *
 * - [Off]: public releases only (`/releases/latest`, non-prerelease).
 * - [PublicBeta]: public releases + tags like `vX.Y.Z-beta.N` / `-rc.N`.
 * - [Developer]: everything published, including `vX.Y.Z-dev.N` / `-alpha.N`.
 */
enum class UpdateChannel(val storageKey: String, val label: String) {
    Off("off", "Off"),
    PublicBeta("public_beta", "Public Beta"),
    Developer("developer", "Developer");

    companion object {
        fun fromStorage(key: String?): UpdateChannel =
            entries.firstOrNull { it.storageKey == key } ?: Off
    }
}
