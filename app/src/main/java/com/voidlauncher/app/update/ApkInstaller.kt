package com.voidlauncher.app.update

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

object ApkInstaller {

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun installPermissionSettingsIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /**
     * Returns a user-facing reason if this APK cannot update the installed app
     * (most often a signing-key mismatch from an older sideload).
     */
    fun updateBlockReason(context: Context, apkFile: File): String? {
        if (!apkFile.exists() || apkFile.length() < 64_000L) {
            return "APK file is missing or incomplete"
        }
        val pm = context.packageManager
        val archive = if (Build.VERSION.SDK_INT >= 33) {
            pm.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
        } ?: return "APK could not be read (invalid package)"

        if (archive.packageName != context.packageName) {
            return "APK package does not match Polar (${archive.packageName})"
        }

        val installed = runCatching {
            if (Build.VERSION.SDK_INT >= 28) {
                pm.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }
        }.getOrNull() ?: return null

        val installedSigs = installedSigningDigests(installed)
        val apkSigs = archiveSigningDigests(pm, apkFile)
        if (installedSigs.isEmpty() || apkSigs.isEmpty()) return null
        if (installedSigs.intersect(apkSigs).isEmpty()) {
            return "Signing key mismatch — uninstall Polar/Void once, then install this APK"
        }
        return null
    }

    fun installApk(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            clipData = ClipData.newRawUri("", uri)
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            putExtra(Intent.EXTRA_RETURN_RESULT, true)
        }

        // OEM package installers often need an explicit URI grant
        val installers = listOf(
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.samsung.android.packageinstaller",
            "com.miui.packageinstaller",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller"
        )
        for (pkg in installers) {
            runCatching {
                context.grantUriPermission(
                    pkg,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }

        context.startActivity(intent)
    }

    private fun installedSigningDigests(info: android.content.pm.PackageInfo): Set<String> {
        return if (Build.VERSION.SDK_INT >= 28) {
            val signingInfo = info.signingInfo ?: return emptySet()
            val sigs = if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
            sigs.map { sha256(it.toByteArray()) }.toSet()
        } else {
            @Suppress("DEPRECATION")
            info.signatures?.map { sha256(it.toByteArray()) }?.toSet().orEmpty()
        }
    }

    private fun archiveSigningDigests(pm: PackageManager, apkFile: File): Set<String> {
        val flags = if (Build.VERSION.SDK_INT >= 28) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        val info = if (Build.VERSION.SDK_INT >= 33) {
            pm.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.PackageInfoFlags.of(flags.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
        } ?: return emptySet()

        // Archive paths need applicationInfo.sourceDir set for some OEMs
        info.applicationInfo?.sourceDir = apkFile.absolutePath
        info.applicationInfo?.publicSourceDir = apkFile.absolutePath
        return installedSigningDigests(info)
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
