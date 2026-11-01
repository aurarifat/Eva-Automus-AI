package com.evai.rifatcoder.data.accessibility

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import java.util.Locale

/**
 * Safely resolves and launches installed Android applications.
 */
class AppLauncherManager(private val context: Context) {

    data class LaunchResult(
        val isSuccess: Boolean,
        val appName: String,
        val packageName: String?,
        val message: String,
    )

    private val commonPackageMap = mapOf(
        "youtube" to "com.google.android.youtube",
        "settings" to "com.android.settings",
        "chrome" to "com.android.chrome",
        "maps" to "com.google.android.apps.maps",
        "google maps" to "com.google.android.apps.maps",
        "play store" to "com.android.vending",
        "google play" to "com.android.vending",
        "clock" to "com.google.android.deskclock",
        "calculator" to "com.google.android.calculator",
        "camera" to "com.google.android.GoogleCamera",
        "messages" to "com.google.android.apps.messaging",
        "gmail" to "com.google.android.gm",
        "photos" to "com.google.android.apps.photos",
    )

    /**
     * Resolves the app by query string or explicit package name and launches it.
     */
    fun launchApp(query: String, explicitPackageName: String? = null): LaunchResult {
        val cleanQuery = query.trim().lowercase(Locale.ROOT)
        val pm: PackageManager = context.packageManager

        // Special handling for Settings
        if (cleanQuery == "settings" || cleanQuery == "system settings") {
            return launchIntent(
                Intent(Settings.ACTION_SETTINGS),
                "Settings",
                "com.android.settings"
            )
        }

        // Special handling for Clock / Alarm
        if (cleanQuery == "clock" || cleanQuery == "alarms") {
            val clockIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
            if (clockIntent.resolveActivity(pm) != null) {
                return launchIntent(clockIntent, "Clock", null)
            }
        }

        // 1. Direct package name provided or mapped
        val targetPackage = explicitPackageName
            ?: commonPackageMap[cleanQuery]
            ?: resolvePackageByFuzzyMatch(cleanQuery, pm)

        if (targetPackage != null) {
            val launchIntent = pm.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                return try {
                    context.startActivity(launchIntent)
                    LaunchResult(
                        isSuccess = true,
                        appName = query,
                        packageName = targetPackage,
                        message = "Successfully launched $query ($targetPackage)."
                    )
                } catch (e: Exception) {
                    LaunchResult(
                        isSuccess = false,
                        appName = query,
                        packageName = targetPackage,
                        message = "Failed to launch $query: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }

        // 2. Fallback to generic web intent if query looks like a web service
        if (cleanQuery == "youtube") {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"))
            return launchIntent(webIntent, "YouTube (Web Fallback)", null)
        }

        return LaunchResult(
            isSuccess = false,
            appName = query,
            packageName = null,
            message = "Application \"$query\" could not be found or has no launchable activity."
        )
    }

    private fun launchIntent(intent: Intent, appName: String, packageName: String?): LaunchResult {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            LaunchResult(
                isSuccess = true,
                appName = appName,
                packageName = packageName,
                message = "Successfully opened $appName."
            )
        } catch (e: Exception) {
            LaunchResult(
                isSuccess = false,
                appName = appName,
                packageName = packageName,
                message = "Failed to open $appName: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    private fun resolvePackageByFuzzyMatch(query: String, pm: PackageManager): String? {
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveList = try {
            pm.queryIntentActivities(mainIntent, 0)
        } catch (_: Exception) {
            emptyList()
        }

        for (resolveInfo in resolveList) {
            val label = resolveInfo.loadLabel(pm).toString().lowercase(Locale.ROOT)
            val pkg = resolveInfo.activityInfo.packageName.lowercase(Locale.ROOT)

            if (label == query || label.contains(query) || pkg.contains(query)) {
                return resolveInfo.activityInfo.packageName
            }
        }
        return null
    }
}
