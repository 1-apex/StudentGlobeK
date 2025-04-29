package com.himanshu03vsk.studentglobek.presentation.activities

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.himanshu03vsk.studentglobek.presentation.components.TopAppBarComponent
import com.himanshu03vsk.studentglobek.ui.theme.StudentGlobeKTheme
import java.text.SimpleDateFormat
import java.util.*

class CreateEventActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StudentGlobeKTheme {
                CreateEventScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen() {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    var eventName by remember { mutableStateOf("") }
    var majorName by remember { mutableStateOf("") }
    var departmentName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var createChatroom by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        TopAppBarComponent("Create New Event")

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = eventName,
            onValueChange = { eventName = it },
            label = { Text("Event Name") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            singleLine = true
        )

        OutlinedTextField(
            value = majorName,
            onValueChange = { majorName = it },
            label = { Text("Major") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            singleLine = true
        )

        OutlinedTextField(
            value = departmentName,
            onValueChange = { departmentName = it },
            label = { Text("Department") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            singleLine = true
        )

        DateRangeSelector(
            onStartDateSelected = { startDate = it },
            onEndDateSelected = { endDate = it }
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            maxLines = 5
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = createChatroom,
                onCheckedChange = { createChatroom = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Text(
                text = "Create Event Chatroom",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                isSaving = true
                val db = FirebaseFirestore.getInstance()
                val userId = auth.currentUser?.uid
                if (eventName.isNotBlank() && startDate.isNotBlank() && endDate.isNotBlank() && userId != null) {
                    val eventRef = db.collection("events").document()
                    val eventData = hashMapOf(
                        "eventId" to eventRef.id,
                        "eventName" to eventName,
                        "startDate" to startDate,
                        "endDate" to endDate,
                        "major" to majorName,
                        "department" to departmentName,
                        "description" to description,
                        "ownerId" to userId,
                        "createdAt" to Timestamp.now()
                    )

                    eventRef.set(eventData)
                        .addOnSuccessListener {
                            if (createChatroom) {
                                val chatroomData = hashMapOf(
                                    "chatroomName" to "$eventName Chat",
                                    "chatroomType" to "Event",
                                    "ownerId" to userId,
                                    "members" to listOf(userId),
                                    "major" to majorName,
                                    "department" to departmentName,
                                    "linkedEventId" to eventRef.id,
                                    "createdAt" to Timestamp.now()
                                )
                                db.collection("chatrooms")
                                    .add(chatroomData)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Event and Chatroom created!", Toast.LENGTH_SHORT).show()
                                        (context as? ComponentActivity)?.finish()
                                    }
                            } else {
                                Toast.makeText(context, "Event created!", Toast.LENGTH_SHORT).show()
                                (context as? ComponentActivity)?.finish()
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                        .addOnCompleteListener {
                            isSaving = false
                        }
                } else {
                    Toast.makeText(context, "Please fill all fields properly.", Toast.LENGTH_SHORT).show()
                    isSaving = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("Create Event")
            }
        }
    }
}

@Composable
fun DateRangeSelector(
    onStartDateSelected: (String) -> Unit,
    onEndDateSelected: (String) -> Unit
) {
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Today's date for validation
    val currentDate = remember { getCurrentDate() }

    fun showDatePicker(onDateSelected: (String) -> Unit) {
        val datePickerDialog = DatePickerDialog(context, { _, year, month, dayOfMonth ->
            val selectedDate = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }.time
            val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate)
            onDateSelected(formattedDate)
        }, 2023, 0, 1)

        val cal = Calendar.getInstance()
        datePickerDialog.datePicker.minDate = cal.timeInMillis
        datePickerDialog.show()
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        OutlinedButton(
            onClick = {
                showDatePicker {
                    if (isValidStartDate(it, currentDate)) {
                        startDate = it
                        onStartDateSelected(it)
                    } else {
                        Toast.makeText(context, "Start date must be after today.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.DateRange, contentDescription = "Select Start Date")
            Spacer(Modifier.width(8.dp))
            Text(if (startDate.isEmpty()) "Start Date" else startDate)
        }

        OutlinedButton(
            onClick = {
                if (startDate.isEmpty()) {
                    Toast.makeText(context, "Please select a Start Date first.", Toast.LENGTH_SHORT).show()
                } else {
                    showDatePicker {
                        if (isValidEndDate(it, startDate)) {
                            endDate = it
                            onEndDateSelected(it)
                        } else {
                            Toast.makeText(context, "End date must be after start date.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.DateRange, contentDescription = "Select End Date")
            Spacer(Modifier.width(8.dp))
            Text(if (endDate.isEmpty()) "End Date" else endDate)
        }

    }
}

fun isValidStartDate(selectedDate: String, currentDate: String): Boolean {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val selected = sdf.parse(selectedDate) ?: return false
    val current = sdf.parse(currentDate) ?: return false
    return selected.after(current)
}

fun isValidEndDate(selectedDate: String, startDate: String): Boolean {
    if (startDate.isEmpty()) return false
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val selected = sdf.parse(selectedDate) ?: return false
    val start = sdf.parse(startDate) ?: return false
    return selected.after(start)
}

fun getCurrentDate(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date())
}
