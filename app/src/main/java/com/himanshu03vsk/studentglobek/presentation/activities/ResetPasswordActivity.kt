package com.himanshu03vsk.studentglobek.presentation.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.himanshu03vsk.studentglobek.ui.theme.StudentGlobeKTheme

class ResetPasswordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            StudentGlobeKTheme {
                ResetPasswordScreen {
                }
            }
        }
    }

    @Composable
    private fun ResetPasswordScreen(onResetSuccess: () -> Unit) {
        var email by remember { mutableStateOf("") }
        var message by remember { mutableStateOf<String?>(null) }
        val context = LocalContext.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Reset Password", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                if (email.isNotBlank()) {
                    FirebaseAuth.getInstance()
                        .sendPasswordResetEmail(email)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                message = "Password reset link sent to your email."
                                onResetSuccess()
                            } else {
                                message = "Error: ${task.exception?.localizedMessage}"
                            }
                        }
                } else {
                    message = "Please enter your email."
                }
            }) {
                Text("Send Reset Link")
            }

            message?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(it)
            }
        }
    }

}