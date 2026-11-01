package com.evai.rifatcoder.data.security

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.evai.rifatcoder.data.accessibility.EvaAccessibilityService

/**
 * Model representing a specific permission level and its real grant state.
 */
data class PermissionItem(
    val id: String,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val isSpecialAccess: Boolean,
    val category: String,
)

/**
 * Central system permission auditor and dispatcher (Phase 15).
 * Validates real system permissions and provides intent launchers to authorize them.
 */
class SystemPermissionManager(private val context: Context) {

    fun getDeclaredRuntimePermissions(): Array<String> {
        val list = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_CALL_LOG,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
            list.add(Manifest.permission.READ_MEDIA_IMAGES)
            list.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(Manifest.permission.BLUETOOTH_SCAN)
            list.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            list.add(Manifest.permission.BLUETOOTH)
            list.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

        return list.toTypedArray()
    }

    fun auditAllPermissions(): List<PermissionItem> {
        val items = mutableListOf<PermissionItem>()

        // 1. Accessibility Service
        items.add(
            PermissionItem(
                id = "accessibility_service",
                title = "Accessibility Service",
                description = "Enables automated screen reading, touch navigation, and cross-app control.",
                isGranted = EvaAccessibilityService.isServiceEnabled(context),
                isSpecialAccess = true,
                category = "Automation",
            )
        )

        // 2. Overlay Window
        items.add(
            PermissionItem(
                id = "overlay_window",
                title = "Overlay Window",
                description = "Required to render the four-side edge glow over active apps.",
                isGranted = Settings.canDrawOverlays(context),
                isSpecialAccess = true,
                category = "System",
            )
        )

        // 3. Notification Listener
        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val isNotifListenerGranted = notifManager?.let {
            val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            enabledListeners?.contains(context.packageName) == true
        } ?: false

        items.add(
            PermissionItem(
                id = "notification_listener",
                title = "Notification Listener",
                description = "Grants real-time status awareness of incoming messages and alerts.",
                isGranted = isNotifListenerGranted,
                isSpecialAccess = true,
                category = "System",
            )
        )

        // 4. Ignore Battery Optimization
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isBatteryOptIgnored = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        items.add(
            PermissionItem(
                id = "battery_optimization",
                title = "Ignore Battery Optimization",
                description = "Prevents background voice and automation services from being terminated.",
                isGranted = isBatteryOptIgnored,
                isSpecialAccess = true,
                category = "System",
            )
        )

        // 5. Modify System Settings
        items.add(
            PermissionItem(
                id = "modify_system_settings",
                title = "Modify System Settings",
                description = "Allows adjusting system brightness, screen timeout, and rotation.",
                isGranted = Settings.System.canWrite(context),
                isSpecialAccess = true,
                category = "System",
            )
        )

        // 6. Do Not Disturb Access
        items.add(
            PermissionItem(
                id = "dnd_access",
                title = "Do Not Disturb Access",
                description = "Permits switching between Priority, Silent, and Normal audio modes.",
                isGranted = notifManager?.isNotificationPolicyAccessGranted == true,
                isSpecialAccess = true,
                category = "System",
            )
        )

        // 7. Microphone
        items.add(
            PermissionItem(
                id = "microphone",
                title = "Microphone",
                description = "Required for real-time speech recognition and hands-free voice commands.",
                isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
                isSpecialAccess = false,
                category = "Media",
            )
        )

        // 8. Camera & Flashlight
        items.add(
            PermissionItem(
                id = "camera",
                title = "Camera & Flashlight",
                description = "Enables hardware flashlight toggling and vision capture.",
                isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
                isSpecialAccess = false,
                category = "Hardware",
            )
        )

        // 9. Contacts
        val contactsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        items.add(
            PermissionItem(
                id = "contacts",
                title = "Contacts",
                description = "Allows calling and messaging contacts by name.",
                isGranted = contactsGranted,
                isSpecialAccess = false,
                category = "Communication",
            )
        )

        // 10. SMS
        val smsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        items.add(
            PermissionItem(
                id = "sms",
                title = "SMS Messages",
                description = "Enables reading and sending text messages.",
                isGranted = smsGranted,
                isSpecialAccess = false,
                category = "Communication",
            )
        )

        // 11. Call Log
        items.add(
            PermissionItem(
                id = "call_log",
                title = "Call Log",
                description = "Allows checking recent phone calls.",
                isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED,
                isSpecialAccess = false,
                category = "Communication",
            )
        )

        // 12. Calendar
        val calGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
        items.add(
            PermissionItem(
                id = "calendar",
                title = "Calendar",
                description = "Allows scheduling, updating, and querying calendar events.",
                isGranted = calGranted,
                isSpecialAccess = false,
                category = "Productivity",
            )
        )

        // 13. Location
        val locGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        items.add(
            PermissionItem(
                id = "location",
                title = "Fine Location",
                description = "Required for nearby place searches, local weather, and map navigation.",
                isGranted = locGranted,
                isSpecialAccess = false,
                category = "Location",
            )
        )

        // 14. Bluetooth
        val btGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }
        items.add(
            PermissionItem(
                id = "bluetooth",
                title = "Bluetooth Controls",
                description = "Enables auditing paired devices and toggling Bluetooth state.",
                isGranted = btGranted,
                isSpecialAccess = false,
                category = "Connectivity",
            )
        )

        return items
    }

    /**
     * Launches the exact system Settings screen for a specific special permission.
     */
    fun openSpecialPermissionSettings(permissionId: String): Boolean {
        return try {
            val intent = when (permissionId) {
                "accessibility_service" -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                "overlay_window" -> Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                "notification_listener" -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                "battery_optimization" -> Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:${context.packageName}")
                )
                "modify_system_settings" -> Intent(
                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:${context.packageName}")
                )
                "dnd_access" -> Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                else -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
            }.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
