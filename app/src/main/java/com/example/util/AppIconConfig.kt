package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.example.R

/**
 * Utility helper for managing and verifying application launcher icon configuration.
 */
object AppIconConfig {
    
    /**
     * Icon asset resource ID currently configured as the launcher foreground drawable.
     */
    val foregroundDrawableResId: Int = R.drawable.img_app_icon_1786194221110

    /**
     * Verifies whether the main launcher activity is enabled and active.
     */
    fun isLauncherEnabled(context: Context): Boolean {
        val packageManager = context.packageManager
        val componentName = ComponentName(context, "com.example.MainActivity")
        val state = packageManager.getComponentEnabledSetting(componentName)
        return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
    }
}
