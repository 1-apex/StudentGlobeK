package com.himanshu03vsk.studentglobek.presentation.activities

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
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

    val dateFormat = SimpleDateFormat("MM-dd-yyyy", Locale.US)

    // Function to show DatePicker dialog
    fun showDatePickerDialog(onDateSet: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val date = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }.time
                val formattedDate = dateFormat.format(date) // Format the date
                onDateSet(formattedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Edit Event") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Event Name Field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Event Name") },
                modifier = Modifier.fillMaxWidth() // Ensure full width
            )

            // Description Field
            OutlinedTextField(
                value = desc,
                onValueChange = { desc = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth() // Ensure full width
            )

            // Start Date Picker Button
            Button(
                onClick = {
                    showDatePickerDialog { formattedDate ->
                        start = formattedDate
                    }
                },
                modifier = Modifier.fillMaxWidth() // Ensure full width
            ) {
                Text(text = if (start.isEmpty()) "Select Start Date" else "Start Date: $start")
            }

            // End Date Picker Button
            Button(
                onClick = {
                    showDatePickerDialog { formattedDate ->
                        end = formattedDate
                    }
                },
                modifier = Modifier.fillMaxWidth() // Ensure full width
            ) {
                Text(text = if (end.isEmpty()) "Select End Date" else "End Date: $end")
            }

            // Major Field
            OutlinedTextField(
                value = maj,
                onValueChange = { maj = it },
                label = { Text("Major") },
                modifier = Modifier.fillMaxWidth() // Ensure full width
            )

            // Department Field
            OutlinedTextField(
                value = dept,
                onValueChange = { dept = it },
                label = { Text("Department") },
                modifier = Modifier.fillMaxWidth() // Ensure full width
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save Changes Button
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
                    FirebaseFirestore.getInstance()
                        .collection("events")
                        .document(eventId)
                        .update(updates)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Event updated!", Toast.LENGTH_SHORT).show()
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
                modifier = Modifier.fillMaxWidth() // Ensure full width
            ) {
                Text(if (isSaving) "Saving..." else "Save Changes")
            }
        }
    }
}
