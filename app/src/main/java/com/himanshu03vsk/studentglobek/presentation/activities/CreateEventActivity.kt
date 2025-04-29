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
import com.himanshu03vsk.studentglobek.ui.theme.StudentGlobeKTheme
import java.text.SimpleDateFormat
import java.util.*

class CreateEventActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
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

    val dateFormat = SimpleDateFormat("MM-dd-yyyy", Locale.getDefault())

    fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selected = Calendar.getInstance()
                selected.set(year, month, dayOfMonth)
                onDateSelected(dateFormat.format(selected.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Event") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { showDatePicker { startDate = it } },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Select Start Date")
                    Spacer(Modifier.width(8.dp))
                    Text(if (startDate.isEmpty()) "Start Date" else startDate)
                }

                OutlinedButton(
                    onClick = { showDatePicker { endDate = it } },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Select End Date")
                    Spacer(Modifier.width(8.dp))
                    Text(if (endDate.isEmpty()) "End Date" else endDate)
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                maxLines = 5
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    isSaving = true
                    val db = FirebaseFirestore.getInstance()
                    val userId = auth.currentUser?.uid
                    if (!eventName.isBlank() && userId != null) {
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
                                Toast.makeText(context, "Event Created Successfully!", Toast.LENGTH_SHORT).show()
                                (context as? ComponentActivity)?.finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Failed: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                            .addOnCompleteListener {
                                isSaving = false
                            }
                    } else {
                        Toast.makeText(context, "Fill all fields properly", Toast.LENGTH_SHORT).show()
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
}
