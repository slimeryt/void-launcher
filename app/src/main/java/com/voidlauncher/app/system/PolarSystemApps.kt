package com.voidlauncher.app.system

object PolarSystemApps {
    const val PhoneActivity = "com.voidlauncher.app.system.phone.PolarPhoneActivity"
    const val MessagesActivity = "com.voidlauncher.app.system.messages.PolarMessagesActivity"

    fun keys(packageName: String): List<String> = listOf(
        "$packageName/$PhoneActivity",
        "$packageName/$MessagesActivity"
    )
}
