package io.github.mradzinski.holler.sample.android

import android.app.Application
import io.github.mradzinski.holler.sample.startSample

/** The Android sample's application: it starts the shared module's dependency injection once per process. */
class SampleApplication : Application() {

    /** Starts dependency injection before any activity is created. */
    override fun onCreate() {
        super.onCreate()

        startSample(context = this)
    }
}
