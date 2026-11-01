package com.evai.rifatcoder.data.accessibility

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.provider.AlarmClock
import com.evai.rifatcoder.domain.automation.DeviceStatusType
import com.evai.rifatcoder.domain.automation.VolumeDirection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles device-level hardware controls (Audio volume, Alarms, Flashlight, Battery & System telemetry).
 */
class DeviceControlManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    /**
     * Adjusts the device audio volume.
     */
    fun adjustVolume(direction: VolumeDirection, streamType: Int = AudioManager.STREAM_MUSIC): String {
        val am = audioManager ?: return "Audio service is unavailable."

        return try {
            when (direction) {
                VolumeDirection.UP -> {
                    am.adjustStreamVolume(streamType, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    val current = am.getStreamVolume(streamType)
                    val max = am.getStreamMaxVolume(streamType)
                    "Volume raised ($current/$max)."
                }
                VolumeDirection.DOWN -> {
                    am.adjustStreamVolume(streamType, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                    val current = am.getStreamVolume(streamType)
                    val max = am.getStreamMaxVolume(streamType)
                    "Volume lowered ($current/$max)."
                }
                VolumeDirection.MUTE -> {
                    am.adjustStreamVolume(streamType, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
                    "Volume muted."
                }
                VolumeDirection.UNMUTE -> {
                    am.adjustStreamVolume(streamType, AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI)
                    val current = am.getStreamVolume(streamType)
                    "Volume unmuted ($current)."
                }
            }
        } catch (e: Exception) {
            "Failed to adjust volume: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    /**
     * Sets a system alarm via official Android AlarmClock intent.
     */
    fun setAlarm(hour: Int, minute: Int, message: String): String {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            "Alarm scheduled for %02d:%02d (%s).".format(hour, minute, message)
        } catch (e: Exception) {
            "Failed to set alarm: ${e.localizedMessage ?: "Alarm app not available"}"
        }
    }

    /**
     * Toggles the device flashlight.
     */
    fun toggleFlashlight(enabled: Boolean): String {
        val cm = cameraManager ?: return "Camera service unavailable for flashlight."
        return try {
            val cameraId = cm.cameraIdList.firstOrNull { id ->
                val chars = cm.getCameraCharacteristics(id)
                chars.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return "No camera with flash hardware detected."

            cm.setTorchMode(cameraId, enabled)
            if (enabled) "Flashlight turned on." else "Flashlight turned off."
        } catch (e: Exception) {
            "Failed to toggle flashlight: ${e.localizedMessage ?: "Hardware error"}"
        }
    }

    /**
     * Retrieves requested device telemetry.
     */
    fun getDeviceStatus(statusType: DeviceStatusType): String {
        return when (statusType) {
            DeviceStatusType.BATTERY -> {
                val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                val batteryStatus = context.registerReceiver(null, batteryFilter)
                val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
                val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                "Battery: $percent% ${if (isCharging) "(Charging)" else "(Discharging)"}."
            }
            DeviceStatusType.NETWORK -> {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                val activeNetwork = cm?.activeNetwork
                val caps = cm?.getNetworkCapabilities(activeNetwork)
                val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

                when {
                    !hasInternet -> "Network: Disconnected / No Internet."
                    isWifi -> "Network: Connected via Wi-Fi."
                    isCellular -> "Network: Connected via Mobile Data."
                    else -> "Network: Connected."
                }
            }
            DeviceStatusType.STORAGE -> {
                val stat = android.os.Environment.getDataDirectory()
                val freeBytes = stat.freeSpace
                val totalBytes = stat.totalSpace
                val freeGb = freeBytes / (1024 * 1024 * 1024)
                val totalGb = totalBytes / (1024 * 1024 * 1024)
                "Storage: $freeGb GB free of $totalGb GB total."
            }
            DeviceStatusType.DATE_TIME -> {
                val format = SimpleDateFormat("EEEE, MMMM d, yyyy • h:mm a", Locale.getDefault())
                "Current date & time: ${format.format(Date())}."
            }
        }
    }
}
