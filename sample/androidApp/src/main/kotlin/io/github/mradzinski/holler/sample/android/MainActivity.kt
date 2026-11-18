package io.github.mradzinski.holler.sample.android

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.mradzinski.holler.sample.App

/** The only screen of the Android sample: it shows the shared app edge to edge. */
class MainActivity : ComponentActivity() {

    /**
     * Sets the shared app as the content of the activity.
     *
     * @param savedInstanceState state saved by a previous instance, unused here.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { App() }
    }

    /**
     * Restyles the system bars when the system switches between light and dark theme. The activity
     * handles that change itself (see `configChanges` in the manifest) and is not recreated, so the
     * bar icons would otherwise keep the colors chosen at launch.
     *
     * @param newConfig the new device configuration.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        enableEdgeToEdge()
    }
}
