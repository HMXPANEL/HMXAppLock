package com.hmx.shield.system.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.hmx.shield.R
import com.hmx.shield.core.security.LockStateManager
import com.hmx.shield.core.security.session.SessionManager
import com.hmx.shield.features.applock.domain.model.RelockPolicy
import com.hmx.shield.features.applock.domain.repository.AppLockRepository
import com.hmx.shield.system.monitoring.ServiceHealthMonitor
import com.hmx.shield.system.overlay.ui.LockOverlayScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class OverlayService : Service(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    // ─── Injected Dependencies ────────────────────────────────────────────────

    @Inject lateinit var lockStateManager: LockStateManager
    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var repository: AppLockRepository
    @Inject lateinit var serviceHealthMonitor: ServiceHealthMonitor

    // ─── Lifecycle for ComposeView inside Service ─────────────────────────────

    private val lifecycleRegistry = LifecycleRegistry(this)

    // Renamed to avoid conflict with the ViewModelStoreOwner property
    private val store = ViewModelStore()

    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    // ─── Internal State ───────────────────────────────────────────────────────

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var heartbeatJob: Job? = null
    private var overlayView: ComposeView? = null
    private lateinit var windowManager: WindowManager
    private var currentPackageName: String? = null

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> sessionManager.onScreenOff()
                Intent.ACTION_SCREEN_ON  -> sessionManager.cleanupExpired()
            }
        }
    }

    companion object {
        const val ACTION_SHOW_OVERLAY       = "com.hmx.shield.ACTION_SHOW_OVERLAY"
        const val ACTION_DISMISS_OVERLAY    = "com.hmx.shield.ACTION_DISMISS_OVERLAY"
        const val ACTION_RESTORE_MONITORING = "com.hmx.shield.ACTION_RESTORE_MONITORING"
        const val EXTRA_PACKAGE_NAME        = "extra_package_name"
        private const val CHANNEL_ID        = "hmx_shield_protection"
        private const val NOTIFICATION_ID   = 1001
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()

        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        startForegroundWithNotification()

        try {
            registerReceiver(
                screenStateReceiver,
                IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_OFF)
                    addAction(Intent.ACTION_SCREEN_ON)
                }
            )
        } catch (e: Exception) {
            // Receiver registration can fail in edge cases — non-fatal
        }

        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        startHeartbeat()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_OVERLAY -> {
                val pkg = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return START_STICKY
                showOverlay(pkg)
            }
            ACTION_DISMISS_OVERLAY    -> dismissOverlay()
            ACTION_RESTORE_MONITORING -> {
                sessionManager.clearAllSessions()
            }
        }
        return START_STICKY
    }

    // ─── Overlay ──────────────────────────────────────────────────────────────

    private fun showOverlay(packageName: String) {
        if (overlayView != null) removeOverlayView()
        currentPackageName = packageName

        serviceScope.launch {
            val lockedApp = repository.getLockedApp(packageName)
            if (lockedApp == null) {
                lockStateManager.dismissOverlay()
                return@launch
            }

            val composeView = ComposeView(this@OverlayService).apply {
                setViewTreeLifecycleOwner(this@OverlayService)
                setViewTreeViewModelStoreOwner(this@OverlayService)
                setViewTreeSavedStateRegistryOwner(this@OverlayService)

                setContent {
                    LockOverlayScreen(
                        packageName  = packageName,
                        appName      = lockedApp.appName,
                        lockType     = lockedApp.lockType,
                        onAuthSuccess = {
                            onAuthSuccess(
                                packageName,
                                lockedApp.relockPolicy,
                                lockedApp.relockTimeoutMs
                            )
                        },
                        onAuthFailed = { lockStateManager.onAuthFailed() }
                    )
                }
            }

            val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                windowType,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_SECURE,
                PixelFormat.TRANSLUCENT
            )

            try {
                windowManager.addView(composeView, params)
                overlayView = composeView
            } catch (e: Exception) {
                lockStateManager.dismissOverlay()
            }
        }
    }

    private fun onAuthSuccess(
        packageName: String,
        policy: RelockPolicy,
        timeoutMs: Long
    ) {
        sessionManager.grantSession(packageName, policy, timeoutMs)
        lockStateManager.onAuthSuccess(packageName)
        removeOverlayView()
        currentPackageName = null
    }

    private fun dismissOverlay() {
        removeOverlayView()
        lockStateManager.dismissOverlay()
        currentPackageName = null
    }

    private fun removeOverlayView() {
        overlayView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        overlayView = null
    }

    // ─── Notification ─────────────────────────────────────────────────────────

    private fun startForegroundWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "HMX Shield Protection",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "App lock protection is active"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, CHANNEL_ID)
        else
            @Suppress("DEPRECATION") Notification.Builder(this)

        return builder
            .setContentTitle("HMX Shield Active")
            .setContentText("Your apps are protected")
            .setSmallIcon(R.drawable.ic_shield_notification)
            .setOngoing(true)
            .build()
    }

    // ─── Heartbeat ────────────────────────────────────────────────────────────

    private fun startHeartbeat() {
        heartbeatJob = serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                serviceHealthMonitor.recordHeartbeat()
                delay(ServiceHealthMonitor.HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    // ─── Teardown ─────────────────────────────────────────────────────────────

    override fun onDestroy() {
        super.onDestroy()
        removeOverlayView()
        try { unregisterReceiver(screenStateReceiver) } catch (_: Exception) {}
        heartbeatJob?.cancel()
        serviceScope.cancel()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
