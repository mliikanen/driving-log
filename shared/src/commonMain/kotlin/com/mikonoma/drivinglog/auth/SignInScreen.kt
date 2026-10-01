package com.mikonoma.drivinglog.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun SignInScreen(processor: SignInProcessor) {
    val state by processor.states.collectAsState()
    SignInContent(state = state, onSignIn = { processor.dispatch(SignInIntent.SignIn) })
}

@Composable
fun SignInContent(state: SignInState, onSignIn: () -> Unit) {
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Driving Log", style = MaterialTheme.typography.headlineMedium)
            if (state.error != null) {
                Text(
                    state.error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp).testTag("sign_in_error"),
                )
            }
            if (state.isSigningIn) {
                CircularProgressIndicator(Modifier.padding(top = 24.dp).testTag("sign_in_progress"))
            } else {
                Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth().padding(top = 24.dp).testTag("sign_in_google")) {
                    Text("Sign in with Google")
                }
            }
        }
    }
}
