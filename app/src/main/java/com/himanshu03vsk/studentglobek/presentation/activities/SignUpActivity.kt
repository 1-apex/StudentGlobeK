package com.himanshu03vsk.studentglobek.presentation.activities

import android.content.Context
import android.content.Intent
import android.icu.text.CaseMap.Lower
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.google.firebase.auth.FirebaseAuth
import com.himanshu03vsk.studentglobek.ui.theme.StudentGlobeKTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.util.Log
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.google.firebase.firestore.FirebaseFirestore

private const val TAG = "Firestore"

class SignUpActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        setContent {
            StudentGlobeKTheme {
                SignUpScreen(auth, this)
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(auth: FirebaseAuth, activity: ComponentActivity) {
    var userName by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var major by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Register") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Create Your Account",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            val fieldModifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)

            OutlinedTextField(
                value = userName, onValueChange = { userName = it },
                label = { Text("Username") }, modifier = fieldModifier
            )
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Full Name") }, modifier = fieldModifier
            )
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = fieldModifier
            )
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = fieldModifier
            )
            OutlinedTextField(
                value = major, onValueChange = { major = it },
                label = { Text("Major") }, modifier = fieldModifier
            )
            OutlinedTextField(
                value = department, onValueChange = { department = it },
                label = { Text("Department") }, modifier = fieldModifier
            )
            OutlinedTextField(
                value = phoneNumber, onValueChange = { phoneNumber = it },
                label = { Text("Phone Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = fieldModifier
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (validateInput(userName, name, email, password, context)) {
                        isLoading = true
                        registerUser(
                            auth, userName, name, email, password,
                            major, department, phoneNumber, context, activity
                        ) {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text("Register")
                }
            }
        }
    }
}


private fun validateInput(userName: String, name: String, email: String, password: String, context: Context): Boolean {
    if (userName.isEmpty() || name.isEmpty() || email.isEmpty() || password.isEmpty()) {
        Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
        return false
    }
    if (password.length < 6) {
        Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
        return false
    }
    return true
}

private fun registerUser(
    auth: FirebaseAuth,
    userName: String,
    name: String,
    email: String,
    password: String,
    major: String,
    department: String,
    phoneNumber: String,
    context: Context,
    activity: ComponentActivity,
    onResult: () -> Unit
) {
    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener(activity) { task ->
            onResult()
            if (task.isSuccessful) {
                val user = auth.currentUser
                user?.let {
                    saveUserData(auth, userName, name, email, major, department, phoneNumber)
                }
                Toast.makeText(context, "Registration successful", Toast.LENGTH_SHORT).show()
                context.startActivity(Intent(context, LoginActivity::class.java))
                activity.finish()
            } else {
                Log.w(TAG, "createUserWithEmail:failure", task.exception)
                Toast.makeText(context, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
}

private fun String.toTitleCase(): String {
    return this.split(" ")
        .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
}

private fun saveUserData(auth: FirebaseAuth, userName: String, name: String, email: String, major: String, department: String, phoneNumber: String) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    if (currentUser != null) {
        val user = hashMapOf(
            "userName" to userName,
            "userID" to currentUser.uid,
            "name" to name.toTitleCase(),
            "email" to email,
            "major" to major.toTitleCase(),
            "department" to department.toTitleCase(),
            "phNumber" to phoneNumber,
            "nameLowerCase" to name.lowercase()
        )

        db.collection("users").document(currentUser.uid)
            .set(user)
            .addOnSuccessListener {
                Log.d(TAG, "User successfully created!")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error creating user", e)
            }
    } else {
        Log.e(TAG, "Error: No authenticated user found.")
    }
}
