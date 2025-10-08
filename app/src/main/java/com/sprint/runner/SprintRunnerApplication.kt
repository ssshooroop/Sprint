package com.sprint.runner

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SprintRunnerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize any app-wide dependencies here
    }
}