package com.voidlauncher.app.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.voidlauncher.app.util.ComponentKey
import com.voidlauncher.app.util.PendingLaunchBounds
import com.voidlauncher.app.util.PolarActivityOptions
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
                val component = ComponentName(info.activityInfo.packageName, info.activityInfo.name)
                val appInfo = try {
                    pm.getApplicationInfo(component.packageName, 0)
                } catch (_: Exception) {
                    null
                }
                AppInfo(
                    label = info.loadLabel(pm).toString(),
                    packageName = component.packageName,
                    activityName = component.className,
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
        val intent = Intent().apply {
            component = ComponentName(app.packageName, app.activityName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent, PolarActivityOptions.bundle(PendingLaunchBounds.take(app.key)))
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
