package com.example.kosmos

import android.app.Application
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class KosmosApplication : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}