package com.himanshu03vsk.studentglobek.presentation.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

    // Check if user already registered
    LaunchedEffect(eventId) {
        currentUserId?.let { uid ->
            val doc = FirebaseFirestore.getInstance().collection("events").document(eventId).get().await()
            val registeredUsers = doc.get("registeredUsers") as? List<*> ?: emptyList<Any>()
            isRegistered = uid in registeredUsers
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Event Details") },
                actions = {
                    if (currentUserId == ownerId) {
                        IconButton(onClick = { showEditMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Edit/Delete")
                        }

                        DropdownMenu(
                            expanded = showEditMenu,
                            onDismissRequest = { showEditMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Event") },
                                onClick = {
                                    showEditMenu = false
                                    Toast.makeText(context, "Edit Event clicked", Toast.LENGTH_SHORT).show()
                                    // Navigate to Edit screen here if needed
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
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = eventName, style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Major: $major")
                Text("Department: $department")
                Text("Start: $startDate")
                Text("End: $endDate")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Description:\n$description")
            }

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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = if (isRegistering) "Registering..." else "Register")
                }
            } else if (currentUserId != ownerId) {
                Text(
                    text = "You are registered for this event.",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Delete Confirmation Dialog
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