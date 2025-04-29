package com.himanshu03vsk.studentglobek.presentation.activities

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
                Scaffold { innerPadding ->
                    CreateChatroomScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun CreateChatroomScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val keyboardController = LocalSoftwareKeyboardController.current

    var chatroomName by remember { mutableStateOf("") }
    var major by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var selectedChatroomType by remember { mutableStateOf("Semester") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBarComponent("Create New Chatroom")

        Spacer(modifier = Modifier.height(24.dp))

        TextFieldInputFunction(
            label = "Chatroom Name",
            value = chatroomName,
            onValueChange = { chatroomName = it },
        )


        Spacer(modifier = Modifier.height(16.dp))

        ChatroomTypeRadioButtons(
            selectedOption = selectedChatroomType,
            onOptionSelected = { selectedChatroomType = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextFieldInputFunction(
            label = "Major",
            value = major,
            onValueChange = { major = it },
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextFieldInputFunction(
            label = "Department",
            value = department,
            onValueChange = { department = it }
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (validateInput(chatroomName, major, department)) {
                    isLoading = true
                    createChatroom(
                        context = context,
                        firestore = firestore,
                        auth = auth,
                        chatroomName = chatroomName,
                        chatroomType = selectedChatroomType,
                        major = major,
                        department = department,
                        onSuccess = {
                            isLoading = false
                            keyboardController?.hide()
                            (context as? ComponentActivity)?.finish()
                        },
                        onError = {
                            isLoading = false
                        }
                    )
                } else {
                    Toast.makeText(context, "Please fill all fields.", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
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

    firestore.collection("chatrooms")
        .whereEqualTo("ownerId", currentUser.uid)
        .get()
        .addOnSuccessListener { querySnapshot ->
            if (querySnapshot.size() >= 4) {
                Toast.makeText(context, "Maximum 4 chatrooms allowed.", Toast.LENGTH_SHORT).show()
                onError(Exception("Limit reached"))
                return@addOnSuccessListener
            }

            val calendar = Calendar.getInstance()
            val expiryTimestamp = when (chatroomType) {
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
                "expiry" to Timestamp(expiryTimestamp / 1000, 0)
            )

            firestore.collection("chatrooms")
                .add(chatroomData)
                .addOnSuccessListener {
                    Toast.makeText(context, "Chatroom created successfully.", Toast.LENGTH_SHORT).show()
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    onError(e)
                }
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Error checking limit: ${e.message}", Toast.LENGTH_SHORT).show()
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
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp)
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
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = option,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
