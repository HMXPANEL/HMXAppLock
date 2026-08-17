package com.hmx.shield.system.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.hmx.shield.core.Constants.EXTRA_LOCK_APP_NAME
import com.hmx.shield.core.Constants.EXTRA_LOCK_PACKAGE
import com.hmx.shield.core.util.PackageUtils
import com.hmx.shield.core.security.LockedAppCache
import com.hmx.shield.core.security.SessionManager
import com.hmx.shield.crash.CrashContextHolder
import com.hmx.shield.features.authentication.LockActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Detects the foreground app via accessibility window events and launches the full
 * screen lock overlay ([LockActivity]) when a protected app is opened without an
 * active unlock session. This is the primary detection mechanism; UsageStats is a
 * documented secondary fallback handled elsewhere.
 */
@AndroidEntryPoint
class AppLockAccessibilityService : AccessibilityService() {

    @Inject lateinit var cache: LockedAppCache
    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var crashContextHolder: CrashContextHolder

    private var lastPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = (serviceInfo ?: AccessibilityServiceInfo()).apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        crashContextHolder.setProtectionState("Accessibility active")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg == packageName) return
        if (pkg == lastPackage) return
        lastPackage = pkg

        // INSTANT policy sessions are dropped once the protected app leaves focus.
        sessionManager.onForegroundChanged(pkg)

        if (cache.isLocked(pkg) && !sessionManager.isUnlocked(pkg)) {
            launchLock(pkg)
        }
    }

    private fun launchLock(pkg: String) {
        val intent = Intent(this, LockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_LOCK_PACKAGE, pkg)
            putExtra(EXTRA_LOCK_APP_NAME, PackageUtils.getAppName(this, pkg))
        }
        runCatching { startActivity(intent) }
    }

    override fun onInterrupt() {
        crashContextHolder.setProtectionState("Accessibility interrupted")
    }
}
