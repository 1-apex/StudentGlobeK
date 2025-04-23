package com.himanshu03vsk.studentglobek.presentation.activities

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.foundation.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.himanshu03vsk.studentglobek.presentation.components.TextFieldInputFunction
import com.himanshu03vsk.studentglobek.presentation.components.TopAppBarComponent
import com.himanshu03vsk.studentglobek.ui.theme.StudentGlobeKTheme
import java.util.Calendar

class CreateChatroomActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StudentGlobeKTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CreateChatroomScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
@Composable
fun CreateChatroomScreen(modifier: Modifier) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    var chatroomName by remember { mutableStateOf("") }
    var major by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var selectedChatroomType by remember { mutableStateOf("Semester") }
    var isLoading by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    // Wrap the screen layout to prevent elements from shifting when the keyboard is open
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .imePadding(), // Ensures padding for keyboard space
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBarComponent("Create New Chatroom")

        Spacer(modifier = Modifier.height(24.dp))

        // Chatroom name text field
        TextFieldInputFunction(
            label = "Chatroom Name",
            value = chatroomName,
            onValueChange = { chatroomName = it },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Chatroom type radio buttons
        ChatroomTypeRadioButtons(
            selectedOption = selectedChatroomType,
            onOptionSelected = { selectedChatroomType = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Major text field
        TextFieldInputFunction(
            label = "Major",
            value = major,
            onValueChange = { major = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Department text field
        TextFieldInputFunction(
            label = "Department",
            value = department,
            onValueChange = { department = it }
        )

        Spacer(modifier = Modifier.weight(1f)) // Flexible space to push button to bottom

        // Create chatroom button
        Button(
            onClick = {
                if (validateInput(chatroomName, major, department)) {
                    createChatroom(
                        context,
                        firestore = firestore,
                        auth = auth,
                        chatroomName = chatroomName,
                        chatroomType = selectedChatroomType,
                        major = major,
                        department = department,
                        onSuccess = {
                            isLoading = false
                            keyboardController?.hide() // Hide keyboard after successful creation
                            (context as? ComponentActivity)?.finish()
                        },
                        onError = { error ->
                            isLoading = false
                            Log.e("CreateChatroom", "Error creating chatroom", error)
                        }
                    )
                    isLoading = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("Create Chatroom")
            }
        }
    }
}

private fun validateInput(chatroomName: String, major: String, department: String): Boolean {
    return chatroomName.isNotBlank() && major.isNotBlank() && department.isNotBlank()
}

private fun createChatroom(
    context: Context,
    firestore: FirebaseFirestore,
    auth: FirebaseAuth,
    chatroomName: String,
    chatroomType: String,
    major: String,
    department: String,
    onSuccess: () -> Unit,
    onError: (Exception) -> Unit
) {
    val currentUser = auth.currentUser

    if (currentUser == null) {
        onError(Exception("User not authenticated"))
        return
    }

    // Check chatroom limit (max 4 per user)
    firestore.collection("chatrooms")
        .whereEqualTo("ownerId", currentUser.uid)
        .get()
        .addOnSuccessListener { querySnapshot ->
            if (querySnapshot.size() >= 4) {
                Toast.makeText(
                    context,
                    "Maximum limit of 4 chatrooms reached",
                    Toast.LENGTH_SHORT
                ).show()
                onError(Exception("Chatroom limit reached"))
                return@addOnSuccessListener
            }

            // Determine expiry time based on chatroom type
            val calendar = Calendar.getInstance()
            val typeDurationInMillis: Long = when (chatroomType) {
                "Semester" -> {
                    calendar.add(Calendar.MONTH, 4)
                    calendar.timeInMillis
                }
                "Event" -> {
                    calendar.add(Calendar.WEEK_OF_YEAR, 4)
                    calendar.timeInMillis
                }
                else -> {
                    calendar.add(Calendar.DAY_OF_YEAR, 30)
                    calendar.timeInMillis
                }
            }

            val chatroomData = hashMapOf(
                "chatroomName" to chatroomName,
                "chatroomType" to chatroomType,
                "ownerId" to currentUser.uid,
                "members" to listOf(currentUser.uid),
                "major" to major,
                "department" to department,
                "createdAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now(),
                "expiry" to Timestamp(typeDurationInMillis / 1000, 0)
            )

            // Create chatroom in Firestore
            firestore.collection("chatrooms")
                .add(chatroomData)
                .addOnSuccessListener {
                    Log.d("CreateChatroom", "Chatroom created successfully!")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.e("CreateChatroom", "Error creating chatroom", e)
                    onError(e)
                }
        }
        .addOnFailureListener { e ->
            Log.e("CreateChatroom", "Error checking chatroom limit", e)
            onError(e)
        }
}



@Composable
fun ChatroomTypeRadioButtons(
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    val options = listOf("Semester", "Event")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.medium
            )
    ) {
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOptionSelected(option) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedOption == option,
                    onClick = { onOptionSelected(option) }
                )
                Text(
                    text = option,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}