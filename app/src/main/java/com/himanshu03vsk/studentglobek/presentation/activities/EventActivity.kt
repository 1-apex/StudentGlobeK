package com.himanshu03vsk.studentglobek.presentation.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.himanshu03vsk.studentglobek.ui.theme.StudentGlobeKTheme
import kotlinx.coroutines.tasks.await
import java.security.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

class EventActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val eventId = intent.getStringExtra("eventId") ?: ""
        val eventName = intent.getStringExtra("eventName") ?: "Event"
        val description = intent.getStringExtra("description") ?: ""
        val startDate = intent.getStringExtra("startDate") ?: ""
        val endDate = intent.getStringExtra("endDate") ?: ""
        val major = intent.getStringExtra("major") ?: ""
        val department = intent.getStringExtra("department") ?: ""
        val ownerId = intent.getStringExtra("ownerId") ?: ""

        setContent {
            StudentGlobeKTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    EventDetailScreen(
                        eventId = eventId,
                        eventName = eventName,
                        description = description,
                        startDate = startDate,
                        endDate = endDate,
                        major = major,
                        department = department,
                        ownerId = ownerId
                    )
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: String,
    eventName: String,
    description: String,
    startDate: String,
    endDate: String,
    major: String,
    department: String,
    ownerId: String
) {
    val context = LocalContext.current
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var isRegistering by remember { mutableStateOf(false) }
    var isRegistered by remember { mutableStateOf(false) }
    var showEditMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        currentUserId?.let { uid ->
            val doc = FirebaseFirestore.getInstance().collection("events").document(eventId).get().await()
            val registeredUsers = doc.get("registeredUsers") as? List<*> ?: emptyList<Any>()
            isRegistered = uid in registeredUsers
        }
    }

    // Format the date for better readability
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
//    val formattedStartDate = formatDate(startDate, dateFormat)
//    val formattedEndDate = formatDate(endDate, dateFormat)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Event Details",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    if (currentUserId == ownerId) {
                        IconButton(onClick = { showEditMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Edit/Delete")
                        }

                        // Smooth dropdown animation
                        DropdownMenu(
                            expanded = showEditMenu,
                            onDismissRequest = { showEditMenu = false },
                            modifier = Modifier.animateContentSize()
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Event") },
                                onClick = {
                                    showEditMenu = false
                                    val intent = Intent(context, EditEventActivity::class.java).apply {
                                        putExtra("eventId", eventId)
                                        putExtra("eventName", eventName)
                                        putExtra("description", description)
                                        putExtra("startDate", startDate)
                                        putExtra("endDate", endDate)
                                        putExtra("major", major)
                                        putExtra("department", department)
                                    }
                                    context.startActivity(intent)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Delete Event") },
                                onClick = {
                                    showEditMenu = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Event Name
            Text(
                text = eventName,
                style = MaterialTheme.typography.headlineLarge,  // Increased size for title
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Major and Department under the event title
            InfoRow(label = "Major", value = major)
            InfoRow(label = "Department", value = department)

            // Start Date and End Date
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoRow(label = "Start Date", value = startDate)
                Spacer(modifier = Modifier.width(16.dp))
                InfoRow(label = "End Date", value = endDate)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description as a separate background card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Register Button
            if (!isRegistered && currentUserId != ownerId) {
                Button(
                    onClick = {
                        isRegistering = true
                        currentUserId?.let { uid ->
                            FirebaseFirestore.getInstance().collection("events")
                                .document(eventId)
                                .update("registeredUsers", FieldValue.arrayUnion(uid))
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Registered successfully!", Toast.LENGTH_SHORT).show()
                                    isRegistered = true
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Failed to register: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                                .addOnCompleteListener {
                                    isRegistering = false
                                }
                        }
                    },
                    enabled = !isRegistering,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text(text = if (isRegistering) "Registering..." else "Register")
                }
            } else if (currentUserId != ownerId) {
                Text(
                    text = "You are registered for this event.",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // Delete confirmation dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Event") },
                text = { Text("Are you sure you want to delete this event? This action cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        FirebaseFirestore.getInstance().collection("events")
                            .document(eventId)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(context, "Event deleted", Toast.LENGTH_SHORT).show()
                                (context as? ComponentActivity)?.finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Failed to delete event", Toast.LENGTH_SHORT).show()
                            }
                        showDeleteDialog = false
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// Utility function to print date as-is based on its type (String or Timestamp)
//fun printDateAsIs(date: Any): String {
//    return when (date) {
//        is Timestamp -> {
//            // If it's a Firebase Timestamp, convert it to a String (raw representation)
//            date.toDate().toString()
//        }
//        is String -> {
//            // If it's already a string, return it as is
//            date
//        }
//        else -> date.toString() // Default case, just return the string representation
//    }
//}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
