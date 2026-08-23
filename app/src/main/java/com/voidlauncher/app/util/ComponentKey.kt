package com.voidlauncher.app.util

import android.content.ComponentName

/** Canonical `package/class` keys for launcher grid items and dock slots. */
object ComponentKey {
    fun of(packageName: String, activityName: String): String {
        val cn = ComponentName(packageName, activityName)
        return "${cn.packageName}/${cn.className}"
    }

    fun normalize(key: String): String {
        val slash = key.indexOf('/')
        if (slash <= 0 || slash >= key.lastIndex) return key
        return of(key.substring(0, slash), key.substring(slash + 1))
    }
}
