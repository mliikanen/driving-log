package com.mikonoma.drivinglog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // Lets UI tests (Maestro) find elements by their test tags, as resource ids.
            App(
                graph = (application as DrivingLogApplication).graph,
                modifier = Modifier.semantics { testTagsAsResourceId = true },
            )
        }
    }
}
