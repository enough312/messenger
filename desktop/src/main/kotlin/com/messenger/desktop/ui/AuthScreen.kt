package com.messenger.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.messenger.desktop.state.AuthMode
import com.messenger.desktop.state.DesktopAppState

@Composable
fun AuthScreen(state: DesktopAppState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Messenger Desktop", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = state.baseUrl,
            onValueChange = { state.baseUrl = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Server URL") },
        )
        OutlinedTextField(
            value = state.email,
            onValueChange = { state.email = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") },
        )
        if (state.authMode == AuthMode.REGISTER) {
            OutlinedTextField(
                value = state.username,
                onValueChange = { state.username = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Username") },
            )
            OutlinedTextField(
                value = state.displayName,
                onValueChange = { state.displayName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Display name") },
            )
        }
        OutlinedTextField(
            value = state.password,
            onValueChange = { state.password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
        )
        Button(
            onClick = state::submitAuth,
            enabled = !state.isBusy,
        ) {
            Text(if (state.authMode == AuthMode.LOGIN) "Login" else "Register")
        }
        Button(
            onClick = {
                state.authMode = if (state.authMode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN
            },
        ) {
            Text(if (state.authMode == AuthMode.LOGIN) "Switch to register" else "Switch to login")
        }
        state.infoMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
