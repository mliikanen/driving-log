package com.mikonoma.drivinglog

import android.content.Intent
import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.mikonoma.drivinglog.auth.ActivityResultBridge

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // The status bar always has light icons, because the header behind it is always dark (Petroleum Deep); the navigation bar is over the
        // app background, so its icons follow the light or dark setting.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val app = application as DrivingLogApplication
        app.applyLaunchArguments(intent)
        setContent {
            // Lets UI tests (Maestro) find elements by their test tags, as resource ids.
            App(
                authRepository = app.authRepository,
                graphFor = { uid -> app.graphFor(uid) },
                modifier = Modifier.semantics { testTagsAsResourceId = true },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        (application as DrivingLogApplication).resumeCount.intValue++
    }

    @Deprecated("Deprecated in Java, but this is the only API a classic startActivityForResult call can respond to.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        ActivityResultBridge.onActivityResult(resultCode, data)
    }
}
