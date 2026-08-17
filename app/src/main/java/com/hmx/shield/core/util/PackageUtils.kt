package com.hmx.shield.core.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val icon: Drawable?
)

/**
 * Lists launchable (user-facing) apps using a MAIN/LAUNCHER intent query.
 * This works without QUERY_ALL_PACKAGES on Android 11+ because we only request
 * apps that expose a launcher entry, which is permitted by package visibility rules.
 */
object PackageUtils {
    fun listLaunchableApps(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { resolve ->
                val packageName = resolve.activityInfo.packageName
                if (packageName == context.packageName) return@mapNotNull null
                InstalledApp(
                    packageName = packageName,
                    appName = resolve.loadLabel(pm).toString(),
                    icon = resolve.loadIcon(pm)
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }
    }

    fun getAppName(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (_: Exception) {
            packageName
        }
    }
}
