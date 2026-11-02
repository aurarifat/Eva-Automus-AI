package com.evai.rifatcoder.data.glow

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.evai.rifatcoder.ui.glow.FourSideEdgeGlowCanvas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * System-level window overlay service that renders the four-side edge glow over other apps.
 * Strictly respects Android overlay security and draws only when authorized.
 */
class EdgeGlowOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var overlayComposeView: ComposeView? = null
    private var isViewAttached = false
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var preferencesRepo: EdgeGlowPreferencesRepository

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    // Instantly hide overlay view on display shutoff to eliminate battery consumption
                    overlayComposeView?.visibility = View.GONE
                }
                Intent.ACTION_SCREEN_ON -> {
                    overlayComposeView?.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        preferencesRepo = EdgeGlowPreferencesRepository(this)

        if (!canDrawOverlays(this)) {
            stopSelf()
            return
        }

        setupOverlayWindow()
        registerScreenStateReceiver()
        observeGlowState()
    }

    private fun setupOverlayWindow() {
        if (isViewAttached || overlayComposeView != null) return
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        overlayComposeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@EdgeGlowOverlayService)
            setViewTreeSavedStateRegistryOwner(this@EdgeGlowOverlayService)
            setContent {
                val glowState by EdgeGlowStateHolder.glowState.collectAsState()
                val soundLevel by EdgeGlowStateHolder.soundLevel.collectAsState()
                val prefs by preferencesRepo.preferences.collectAsState()

                FourSideEdgeGlowCanvas(
                    glowState = glowState,
                    soundLevel = soundLevel,
                    preferences = prefs,
                )
            }
        }

        try {
            windowManager?.addView(overlayComposeView, params)
            isViewAttached = true
        } catch (e: Exception) {
            isViewAttached = false
        }
    }

    private fun observeGlowState() {
        serviceScope.launch {
            combine(
                EdgeGlowStateHolder.glowState,
                preferencesRepo.preferences,
            ) { state, prefs ->
                Pair(state, prefs)
            }.collect { (state, prefs) ->
                // Security check: If overlay permission has been revoked by user or system, stop and clean up immediately
                if (!canDrawOverlays(this@EdgeGlowOverlayService)) {
                    stopSelf()
                    return@collect
                }
                if (!prefs.isEnabled || !prefs.isSystemOverlayEnabled || !prefs.shouldGlowForState(state)) {
                    overlayComposeView?.visibility = View.GONE
                } else {
                    overlayComposeView?.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun registerScreenStateReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        serviceScope.cancel()

        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) {
        }

        if (isViewAttached && overlayComposeView != null) {
            try {
                windowManager?.removeView(overlayComposeView)
            } catch (_: Exception) {
            }
            isViewAttached = false
        }
        overlayComposeView = null
        windowManager = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun canDrawOverlays(context: Context): Boolean {
            return Settings.canDrawOverlays(context)
        }

        fun openOverlaySettings(context: Context) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }

        fun startService(context: Context) {
            if (canDrawOverlays(context)) {
                val intent = Intent(context, EdgeGlowOverlayService::class.java)
                try {
                    context.startService(intent)
                } catch (_: Exception) {
                }
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, EdgeGlowOverlayService::class.java)
            try {
                context.stopService(intent)
            } catch (_: Exception) {
            }
        }
    }
}
