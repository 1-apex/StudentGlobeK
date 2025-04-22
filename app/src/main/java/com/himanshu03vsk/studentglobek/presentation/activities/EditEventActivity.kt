package com.himanshu03vsk.studentglobek.presentation.activities

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
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Event Name") })
            OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
            OutlinedTextField(value = start, onValueChange = { start = it }, label = { Text("Start Date") })
            OutlinedTextField(value = end, onValueChange = { end = it }, label = { Text("End Date") })
            OutlinedTextField(value = maj, onValueChange = { maj = it }, label = { Text("Major") })
            OutlinedTextField(value = dept, onValueChange = { dept = it }, label = { Text("Department") })

            Spacer(modifier = Modifier.height(16.dp))

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
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSaving) "Saving..." else "Save Changes")
            }
        }
    }
}
