package com.voidlauncher.app.system

import com.voidlauncher.app.util.ComponentKey

object PolarSystemApps {
    const val PhoneActivity = "com.voidlauncher.app.system.phone.PolarPhoneActivity"
    const val MessagesActivity = "com.voidlauncher.app.system.messages.PolarMessagesActivity"

    fun keys(packageName: String): List<String> = listOf(
        ComponentKey.of(packageName, PhoneActivity),
        ComponentKey.of(packageName, MessagesActivity)
    )
}
