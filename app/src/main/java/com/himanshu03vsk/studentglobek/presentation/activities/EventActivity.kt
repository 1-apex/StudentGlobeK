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
import com.himanshu03vsk.studentglobek.presentation.activities.EditEventActivity

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event Details", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    if (currentUserId == ownerId) {
                        IconButton(onClick = { showEditMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                        }

                        DropdownMenu(
                            expanded = showEditMenu,
                            onDismissRequest = { showEditMenu = false }
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
                                text = { Text("Delete Event", color = MaterialTheme.colorScheme.error) },
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            Text(
                text = eventName,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            InfoRow(label = "Major", value = major)
            InfoRow(label = "Department", value = department)

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                InfoRow(label = "Start", value = startDate)
                InfoRow(label = "End", value = endDate)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = "Description", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = description, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.weight(1f))

            if (currentUserId != ownerId) {
                if (isRegistered) {
                    Text(
                        text = "You are registered for this event.",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else {
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
                                        Toast.makeText(context, "Registration failed: ${it.message}", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnCompleteListener {
                                        isRegistering = false
                                    }
                            }
                        },
                        enabled = !isRegistering,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isRegistering) "Registering..." else "Register")
                    }
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Event") },
                text = { Text("Are you sure you want to delete this event? This cannot be undone.") },
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

@Composable
fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
