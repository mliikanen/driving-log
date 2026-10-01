package com.mikonoma.drivinglog.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp

/**
 * Stands in for the real Google account picker in the `fake` flavor (design.md decision 7): a plain screen, not a
 * transient popup, so Maestro can drive it by testTag like any other screen in this app, instead of a system
 * surface it can't script.
 */
class TestAccountPickerActivity : ComponentActivity() {
    enum class PickResult { ACCOUNT_A, ACCOUNT_B, CANCELLED, SIMULATED_FAILURE }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    // A separate Activity means a separate Compose composition root — MainActivity's own
                    // testTagsAsResourceId doesn't reach here, so it's set again (found via a real Maestro run).
                    Column(
                        Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }.padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Choose a test account")
                        Button(
                            onClick = { finishWith(PickResult.ACCOUNT_A) },
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).testTag("test_account_a"),
                        ) { Text("Test Account A") }
                        Button(
                            onClick = { finishWith(PickResult.ACCOUNT_B) },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("test_account_b"),
                        ) { Text("Test Account B") }
                        Button(
                            onClick = { finishWith(PickResult.SIMULATED_FAILURE) },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("simulate_failure"),
                        ) { Text("Simulate failure") }
                        Button(
                            onClick = { finishWith(PickResult.CANCELLED) },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("sign_in_cancel"),
                        ) { Text("Cancel") }
                    }
                }
            }
        }
    }

    private fun finishWith(result: PickResult) {
        setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_RESULT, result.name))
        finish()
    }

    companion object {
        private const val EXTRA_RESULT = "result"

        suspend fun pick(activity: Activity): PickResult {
            val (resultCode, data) = ActivityResultBridge.launchForResult(activity, Intent(activity, TestAccountPickerActivity::class.java))
            if (resultCode != Activity.RESULT_OK) return PickResult.CANCELLED
            val name = data?.getStringExtra(EXTRA_RESULT) ?: return PickResult.CANCELLED
            return PickResult.valueOf(name)
        }
    }
}
