package com.himanshu03vsk.studentglobek.presentation.activities

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.himanshu03vsk.studentglobek.presentation.components.TextFieldInputFunction
import com.himanshu03vsk.studentglobek.presentation.components.LargeInputBox
import com.himanshu03vsk.studentglobek.presentation.components.TopAppBarComponent
import com.himanshu03vsk.studentglobek.ui.theme.StudentGlobeKTheme
import java.text.SimpleDateFormat
import java.util.*

class CreateEventActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            StudentGlobeKTheme {
                // Layout for creating a chatroom
                var eventName by remember { mutableStateOf("") }
                var majorName by remember { mutableStateOf("") }
                var startDate by remember { mutableStateOf("") }
                var endDate by remember { mutableStateOf("") }
                var descValue by remember { mutableStateOf("") }
                var departmentName by remember { mutableStateOf("") }
                var selectedChatRoomType by remember { mutableStateOf("") }
                var mExpanded by remember { mutableStateOf(false) }

                // Format for displaying the date
                val dateFormat = SimpleDateFormat("MM-dd-yyyy", Locale.US)

                val context = LocalContext.current

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

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TopAppBarComponent("Create an Event")

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Using TextFieldInputFunction to get user input for event name
                        TextFieldInputFunction(
                            label = "Event Name",
                            value = eventName,
                            onValueChange = { eventName = it }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Start Date Picker Button
                        Button(
                            onClick = {
                                showDatePickerDialog { formattedDate ->
                                    startDate = formattedDate
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = if (startDate.isEmpty()) "Select Start Date" else "Start Date: $startDate")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // End Date Picker Button
                        Button(
                            onClick = {
                                showDatePickerDialog { formattedDate ->
                                    endDate = formattedDate
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = if (endDate.isEmpty()) "Select End Date" else "End Date: $endDate")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Major TextField
                        TextFieldInputFunction(
                            label = "Major",
                            value = majorName,
                            onValueChange = { majorName = it }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Department TextField
                        TextFieldInputFunction(
                            label = "Department",
                            value = departmentName,
                            onValueChange = { departmentName = it }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Description TextField
                        LargeInputBox(
                            label = "Description of the Event",
                            value = descValue,
                            onValueChange = { descValue = it }
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = {
                                // Firebase logic

                                val db = FirebaseFirestore.getInstance()
                                val auth = FirebaseAuth.getInstance()
                                val userId = auth.currentUser?.uid

                                if (userId != null && eventName.isNotEmpty()) {
                                    val eventData = hashMapOf(
                                        "eventName" to eventName,
                                        "startDate" to startDate,
                                        "endDate" to endDate,
                                        "major" to majorName,
                                        "department" to departmentName,
                                        "description" to descValue,
                                        "createdAt" to Timestamp.now(),
                                        "ownerId" to userId
                                    )

                                    val eventRef = db.collection("events")
                                        .document() // Create doc with auto ID
                                    val eventId = eventRef.id

                                    eventData["eventId"] = eventId // Store eventId inside doc

                                    eventRef.set(eventData)
                                        .addOnSuccessListener {
                                            Toast.makeText(
                                                context,
                                                "Event Created",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            finish() // Go back after creation
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(
                                                context,
                                                "Failed: ${e.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Missing Event Name or Not Logged In",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier
                                .padding(32.dp)
                                .fillMaxWidth()
                        ) {
                            Text("Create")
                        }
                    }
                }
            }
        }
    }
}
