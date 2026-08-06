package com.voidlauncher.app.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import java.io.File

object ApkInstaller {

    const val ACTION_INSTALL_STATUS = "com.voidlauncher.app.UPDATE_INSTALL_STATUS"
    const val EXTRA_SESSION_ID = "session_id"

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

        val installedCode = if (Build.VERSION.SDK_INT >= 28) {
            installed.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            installed.versionCode.toLong()
        }
        val apkCode = if (Build.VERSION.SDK_INT >= 28) {
            archive.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            archive.versionCode.toLong()
        }
        if (apkCode in 1 until installedCode) {
            return "Downloaded APK is older (build $apkCode) than installed (build $installedCode)"
        }

        val installedSigs = installedSigningDigests(installed)
        val apkSigs = archiveSigningDigests(pm, apkFile)
        if (installedSigs.isEmpty() || apkSigs.isEmpty()) return null
        if (installedSigs.intersect(apkSigs).isEmpty()) {
            return "Signing key mismatch — uninstall Polar/Void once, then install this APK"
        }
        return null
    }

    /**
     * Prefer PackageInstaller sessions — ACTION_VIEW + FileProvider often flashes and dies
     * on Nothing / Android 13+ OEM installers (URI grant / NEW_TASK quirks).
     */
    fun installApk(context: Context, apkFile: File) {
        try {
            installWithSession(context, apkFile)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Install failed: ${e.message ?: e.javaClass.simpleName}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun installWithSession(context: Context, apkFile: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName(context.packageName)
        // Always show the system confirm sheet. NOT_REQUIRED can flash/abort on
        // Nothing OS and other OEMs even for same-cert self-updates.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
        }

        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            apkFile.inputStream().use { input ->
                session.openWrite("Polar.apk", 0, apkFile.length()).use { out ->
                    input.copyTo(out, bufferSize = 64 * 1024)
                    session.fsync(out)
                }
            }

            val callback = Intent(context, InstallResultReceiver::class.java).apply {
                action = ACTION_INSTALL_STATUS
                setPackage(context.packageName)
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE
                } else {
                    0
                }
            val pending = PendingIntent.getBroadcast(context, sessionId, callback, flags)
            session.commit(pending.intentSender)
        }
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

        info.applicationInfo?.sourceDir = apkFile.absolutePath
        info.applicationInfo?.publicSourceDir = apkFile.absolutePath
        return installedSigningDigests(info)
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Handles PackageInstaller session callbacks. Must launch the system confirmation
 * UI on [PackageInstaller.STATUS_PENDING_USER_ACTION] — otherwise install appears to
 * flash a progress bar and immediately vanish.
 */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirm != null) {
                    confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(confirm) }
                        .onFailure {
                            Toast.makeText(
                                context,
                                "Could not open installer: ${it.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                } else {
                    Toast.makeText(context, "Installer confirmation missing", Toast.LENGTH_LONG).show()
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                Toast.makeText(context, "Update installed", Toast.LENGTH_SHORT).show()
            }
            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                Toast.makeText(context, "Install cancelled", Toast.LENGTH_SHORT).show()
            }
            else -> {
                val detail = message?.takeIf { it.isNotBlank() } ?: "status $status"
                Toast.makeText(context, "Install failed: $detail", Toast.LENGTH_LONG).show()
            }
        }
    }
}
