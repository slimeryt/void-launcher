package com.voidlauncher.app.system

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.TelecomManager
import android.provider.Telephony

object PolarRoles {
    const val Phone = "phone"
    const val Messages = "messages"

    fun isPhone(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 29) {
            val rm = context.getSystemService(RoleManager::class.java) ?: return false
            return rm.isRoleHeld(RoleManager.ROLE_DIALER)
        }
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        return telecom?.defaultDialerPackage == context.packageName
    }

    fun isMessages(context: Context): Boolean =
        Telephony.Sms.getDefaultSmsPackage(context) == context.packageName

    fun isCallerId(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 29) return false
        val rm = context.getSystemService(RoleManager::class.java) ?: return false
        return rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    fun requestPhone(activity: Activity): Intent? {
        if (Build.VERSION.SDK_INT >= 29) {
            val rm = activity.getSystemService(RoleManager::class.java) ?: return null
            if (rm.isRoleAvailable(RoleManager.ROLE_DIALER) && !rm.isRoleHeld(RoleManager.ROLE_DIALER)) {
                return rm.createRequestRoleIntent(RoleManager.ROLE_DIALER)
            }
            return null
        }
        return Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
            putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, activity.packageName)
        }
    }

    fun requestMessages(activity: Activity): Intent? {
        if (Build.VERSION.SDK_INT >= 29) {
            val rm = activity.getSystemService(RoleManager::class.java) ?: return null
            if (rm.isRoleAvailable(RoleManager.ROLE_SMS) && !rm.isRoleHeld(RoleManager.ROLE_SMS)) {
                return rm.createRequestRoleIntent(RoleManager.ROLE_SMS)
            }
            return null
        }
        return Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
            putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, activity.packageName)
        }
    }

    fun requestCallerId(activity: Activity): Intent? {
        if (Build.VERSION.SDK_INT < 29) return null
        val rm = activity.getSystemService(RoleManager::class.java) ?: return null
        if (rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
            !rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        ) {
            return rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
        }
        return null
    }
}
