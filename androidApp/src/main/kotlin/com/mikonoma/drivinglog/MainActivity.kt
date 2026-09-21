package com.mikonoma.drivinglog

import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // The status bar always has light icons, because the header behind it is always dark (Petroleum Deep); the navigation bar is over the
        // app background, so its icons follow the light or dark setting.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            // Lets UI tests (Maestro) find elements by their test tags, as resource ids.
            App(
                graph = (application as DrivingLogApplication).graph,
                modifier = Modifier.semantics { testTagsAsResourceId = true },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        (application as DrivingLogApplication).resumeCount.intValue++
    }
}
