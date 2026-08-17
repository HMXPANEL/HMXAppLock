package com.hmx.shield.crash

import com.hmx.shield.crash.model.CrashReport
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportFormatterTest {

    @Test
    fun formatContainsKeySections() {
        val report = CrashReport(
            id = "abc",
            timestamp = 1_700_000_000_000L,
            appVersion = "1.0.0",
            versionCode = 1L,
            buildType = "release",
            androidVersion = "14",
            apiLevel = 34,
            manufacturer = "google",
            model = "Pixel",
            processName = "com.hmx.shield",
            threadName = "main",
            exceptionClass = "java.lang.RuntimeException",
            message = "boom",
            stackTrace = "stack",
            causedBy = null,
            screen = "MainActivity",
            contextInfo = "running"
        )
        val text = CrashReportFormatter.format(report)
        assertTrue(text.contains("HMX SHIELD CRASH REPORT"))
        assertTrue(text.contains("boom"))
        assertTrue(text.contains("MainActivity"))
        assertTrue(text.contains("stack"))
    }
}
