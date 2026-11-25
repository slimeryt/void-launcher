package com.voidlauncher.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Trampoline: Settings lives on Polar Home so liquid glass can sample the
 * real wallpaper layer. This activity only forwards into [MainActivity].
 */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                )
                putExtra(MainActivity.EXTRA_OPEN_SETTINGS, true)
            }
        )
        finish()
    }
}
