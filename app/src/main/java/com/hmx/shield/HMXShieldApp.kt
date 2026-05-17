package com.hmx.shield

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class — entry point for Hilt dependency injection.
 *
 * This is the first class Android instantiates when the app process starts.
 * All Hilt SingletonComponent dependencies are created here.
 */
@HiltAndroidApp
class HMXShieldApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Future: initialize crash reporting, local logging, etc.
    }
}
