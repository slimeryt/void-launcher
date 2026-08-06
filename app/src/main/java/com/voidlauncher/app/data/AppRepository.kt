package com.voidlauncher.app.data

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.voidlauncher.app.util.LauncherWindow
import com.voidlauncher.app.util.PendingLaunchBounds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

    suspend fun loadLaunchableApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)

        resolveInfos
            .asSequence()
            .map { info ->
                val appInfo = try {
                    pm.getApplicationInfo(info.activityInfo.packageName, 0)
                } catch (_: Exception) {
                    null
                }
                AppInfo(
                    label = info.loadLabel(pm).toString(),
                    packageName = info.activityInfo.packageName,
                    activityName = info.activityInfo.name,
                    icon = info.loadIcon(pm),
                    isSystemApp = appInfo?.let {
                        (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    } ?: false
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun launch(app: AppInfo) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setClassName(app.packageName, app.activityName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        context.startActivity(intent, buildLaunchOptions())
    }

    /** iOS-style scale-up from the tapped icon's on-screen position, when available. */
    private fun buildLaunchOptions(): android.os.Bundle? {
        val bounds = PendingLaunchBounds.rect ?: return null
        PendingLaunchBounds.rect = null
        val decor = LauncherWindow.decorView ?: return null
        if (bounds.width() <= 0 || bounds.height() <= 0) return null
        return runCatching {
            ActivityOptions.makeScaleUpAnimation(
                decor,
                bounds.left,
                bounds.top,
                bounds.width(),
                bounds.height()
            ).toBundle()
        }.getOrNull()
    }

    fun openAppInfo(packageName: String) {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.parse("package:$packageName")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun uninstall(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = android.net.Uri.parse("package:$packageName")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
