package com.himanshu03vsk.studentglobek.presentation.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.himanshu03vsk.studentglobek.ui.theme.StudentGlobeKTheme

class LoginActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user != null) {
            startActivity(Intent(this, HomePageActivity::class.java))
            finish()
        }

        setContent {
            StudentGlobeKTheme {
                LoginScreen(auth, this)
            }
        }
    }
}

@Composable
fun LoginScreen(auth: FirebaseAuth, activity: ComponentActivity) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(auth, email, password, context, activity)
            } else {
                Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(8.dp))

//        Button(onClick = {
//            context.startActivity(Intent(context, SignUpActivity::class.java))
//        }) {
//            Text("Register")
//        }

        Text(
            text = "Forgot Password?",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable {
                    val intent = Intent(context, ResetPasswordActivity::class.java)
                    context.startActivity(intent)
                }
        )

    }
}

private fun loginUser(auth: FirebaseAuth, email: String, password: String, context: Context, activity: ComponentActivity) {
    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener(activity) { task ->
            if (task.isSuccessful) {
                Log.d("Login", "signInWithEmail:success")
                context.startActivity(Intent(context, HomePageActivity::class.java))
                activity.finish()
            } else {
                Log.w("Login", "signInWithEmail:failure", task.exception)
                Toast.makeText(context, "Authentication failed.", Toast.LENGTH_SHORT).show()
            }
        }
}
