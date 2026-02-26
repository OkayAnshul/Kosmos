package com.example.kosmos

import android.app.Application
import com.example.kosmos.core.config.AppConfigRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltAndroidApp
class KosmosApplication : Application() {

    @Inject lateinit var appConfigRepository: AppConfigRepository

    override fun onCreate() {
        super.onCreate()
        // Pre-warm remote config + Coil logo cache in the background.
        // StateFlow is already seeded from SharedPreferences so UI never blocks.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            appConfigRepository.refresh()
        }
    }
}
