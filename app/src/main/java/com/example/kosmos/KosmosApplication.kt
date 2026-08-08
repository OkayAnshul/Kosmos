package com.example.kosmos

import android.app.Application
import android.util.Log
import com.example.kosmos.features.settings.presentation.redesign.AppConfigEntryPoint
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


@HiltAndroidApp
class KosmosApplication : Application() {

    companion object {
        private const val TAG = "KosmosApplication"
    }

    override fun onCreate() {
        super.onCreate()

        // Skip network-backed startup work for JVM/Robolectric tests.
        if (isRunningInUnitTestEnvironment()) {
            return
        }

        // Pre-warm remote config + Coil logo cache in the background.
        // StateFlow is already seeded from SharedPreferences so UI never blocks.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            // Skip network-backed startup work in offline demo mode.
            val demoEnabled = getSharedPreferences("kosmos_prefs", MODE_PRIVATE)
                .getBoolean(com.example.kosmos.features.demo.DemoMode.PREFS_KEY, false)
            if (demoEnabled) return@launch

            runCatching {
                EntryPointAccessors.fromApplication(
                    this@KosmosApplication,
                    AppConfigEntryPoint::class.java
                ).appConfigRepository().refresh()
            }.onFailure { error ->
                Log.w(TAG, "App config prewarm skipped: ${error.message}")
            }
        }
    }

    private fun isRunningInUnitTestEnvironment(): Boolean {
        return try {
            Class.forName("org.robolectric.RuntimeEnvironment")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }
}
