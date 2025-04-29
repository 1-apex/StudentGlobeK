package com.himanshu03vsk.studentglobek.presentation.activities

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.himanshu03vsk.studentglobek.ui.theme.StudentGlobeKTheme
import java.text.SimpleDateFormat
import java.util.*

class EditEventActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val eventId = intent.getStringExtra("eventId") ?: ""
        val initialName = intent.getStringExtra("eventName") ?: ""
        val initialDescription = intent.getStringExtra("description") ?: ""
        val initialStart = intent.getStringExtra("startDate") ?: ""
        val initialEnd = intent.getStringExtra("endDate") ?: ""
        val initialMajor = intent.getStringExtra("major") ?: ""
        val initialDept = intent.getStringExtra("department") ?: ""

        setContent {
            StudentGlobeKTheme {
                EditEventScreen(
                    eventId,
                    initialName,
                    initialDescription,
                    initialStart,
                    initialEnd,
                    initialMajor,
                    initialDept
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventScreen(
    eventId: String,
    eventName: String,
    description: String,
    startDate: String,
    endDate: String,
    major: String,
    department: String
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf(eventName) }
    var desc by remember { mutableStateOf(description) }
    var start by remember { mutableStateOf(startDate) }
    var end by remember { mutableStateOf(endDate) }
    var maj by remember { mutableStateOf(major) }
    var dept by remember { mutableStateOf(department) }
    var isSaving by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("MM-dd-yyyy", Locale.getDefault())
    val db = FirebaseFirestore.getInstance()

    fun showDatePickerDialog(onDateSet: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val date = Calendar.getInstance().apply {
                    set(year, month, day)
                }.time
                onDateSet(dateFormat.format(date))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Event", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Event Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = desc,
                onValueChange = { desc = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 5
            )

            OutlinedButton(
                onClick = { showDatePickerDialog { start = it } },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (start.isEmpty()) "Select Start Date" else "Start Date: $start")
            }

            OutlinedButton(
                onClick = { showDatePickerDialog { end = it } },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (end.isEmpty()) "Select End Date" else "End Date: $end")
            }

            OutlinedTextField(
                value = maj,
                onValueChange = { maj = it },
                label = { Text("Major") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = dept,
                onValueChange = { dept = it },
                label = { Text("Department") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    isSaving = true
                    val updates = mapOf(
                        "eventName" to name,
                        "description" to desc,
                        "startDate" to start,
                        "endDate" to end,
                        "major" to maj,
                        "department" to dept
                    )
                    db.collection("events")
                        .document(eventId)
                        .update(updates)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Event updated successfully!", Toast.LENGTH_SHORT).show()
                            (context as? ComponentActivity)?.finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Update failed: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                        .addOnCompleteListener {
                            isSaving = false
                        }
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Save Changes")
                }
            }
        }

        // Delete Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Event") },
                text = { Text("Are you sure you want to delete this event? Its linked chatroom will also be deleted.") },
                confirmButton = {
                    TextButton(onClick = {
                        // Delete event first
                        db.collection("events").document(eventId)
                            .delete()
                            .addOnSuccessListener {
                                // Then delete associated chatroom
                                db.collection("chatrooms")
                                    .whereEqualTo("linkedEventId", eventId)
                                    .get()
                                    .addOnSuccessListener { querySnapshot ->
                                        for (doc in querySnapshot.documents) {
                                            doc.reference.delete()
                                        }
                                        Toast.makeText(context, "Event & Chatroom deleted successfully!", Toast.LENGTH_SHORT).show()
                                        (context as? ComponentActivity)?.finish()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "Failed to delete chatroom: ${it.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Failed to delete event: ${it.message}", Toast.LENGTH_SHORT).show()
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
