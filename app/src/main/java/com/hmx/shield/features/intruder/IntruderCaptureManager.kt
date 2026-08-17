package com.hmx.shield.features.intruder

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.BatteryManager
import android.os.CountDownLatch
import android.os.TimeUnit
import androidx.core.content.ContextCompat
import com.hmx.shield.data.local.repository.IntruderRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Records an intruder event after too many failed unlock attempts. When the CAMERA
 * permission is granted and a front camera exists, a single selfie is captured and
 * stored locally. Every step is wrapped defensively: if anything fails the event is
 * still recorded (with an empty image path) so protection telemetry is never lost
 * and the capture can never crash the lock screen.
 */
@Singleton
class IntruderCaptureManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val intruderRepository: IntruderRepository
) {
    fun capture(packageName: String, attempts: Int) {
        val battery = readBatteryPercent()
        val imagePath = captureSelfie()
        runCatching {
            intruderRepository.record(imagePath ?: "", packageName, attempts, battery)
        }
    }

    private fun readBatteryPercent(): Int = runCatching {
        val intent = context.registerReceiver(null, Intent(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level >= 0 && scale > 0) level * 100 / scale else -1
    }.getOrDefault(-1)

    private fun captureSelfie(): String? = runCatching {
        if (!cameraPermissionGranted() || !hasFrontCamera()) return null
        var camera: Camera? = null
        val latch = CountDownLatch(1)
        var result: String? = null
        try {
            camera = Camera.open(frontCameraId())
            val dir = File(context.filesDir, "intruder").apply { mkdirs() }
            val file = File(dir, "intruder_${System.currentTimeMillis()}.jpg")
            camera.setPreviewTexture(android.graphics.SurfaceTexture(0))
            camera.startPreview()
            camera.takePicture(null, null) { data, _ ->
                try {
                    FileOutputStream(file).use { it.write(data) }
                    result = file.absolutePath
                } catch (_: Exception) {
                    result = null
                }
                latch.countDown()
            }
            latch.await(4, TimeUnit.SECONDS)
        } finally {
            runCatching {
                camera?.stopPreview()
                camera?.release()
            }
        }
        result
    }.getOrNull()

    private fun cameraPermissionGranted(): Boolean =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    private fun hasFrontCamera(): Boolean {
        val count = runCatching { Camera.getNumberOfCameras() }.getOrDefault(0)
        for (i in 0 until count) {
            val info = Camera.CameraInfo()
            runCatching { Camera.getCameraInfo(i, info) }
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) return true
        }
        return false
    }

    private fun frontCameraId(): Int {
        val count = runCatching { Camera.getNumberOfCameras() }.getOrDefault(0)
        for (i in 0 until count) {
            val info = Camera.CameraInfo()
            runCatching { Camera.getCameraInfo(i, info) }
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) return i
        }
        return 0
    }
}
