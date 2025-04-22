package com.himanshu03vsk.studentglobek.presentation.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.himanshu03vsk.studentglobek.ui.theme.StudentGlobeKTheme
import kotlinx.coroutines.tasks.await

class ChatroomActivity : ComponentActivity() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val chatroomId = intent.getStringExtra("chatroomId") ?: ""
        val chatroomName = intent.getStringExtra("chatroomName") ?: "Chatroom"

        setContent {
            StudentGlobeKTheme {
                ChatroomScreen(
                    chatroomId = chatroomId,
                    chatroomName = chatroomName,
                    onLeave = { leaveChatroom(chatroomId) },
                    onDelete = { deleteChatroom(chatroomId) }
                )
            }
        }
    }

    private fun leaveChatroom(chatroomId: String) {
        val uid = auth.currentUser?.uid ?: return

        db.collection("chatrooms").document(chatroomId)
            .update("members", FieldValue.arrayRemove(uid))
            .addOnSuccessListener {
                Toast.makeText(this, "Left chatroom", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to leave chatroom", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteChatroom(chatroomId: String) {
        db.collection("chatrooms").document(chatroomId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Chatroom deleted", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete chatroom", Toast.LENGTH_SHORT).show()
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatroomScreen(
    chatroomId: String,
    chatroomName: String,
    onLeave: () -> Unit,
    onDelete: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    var isOwner by remember { mutableStateOf(false) }

    // Check ownership
    LaunchedEffect(chatroomId) {
        val uid = auth.currentUser?.uid ?: return@LaunchedEffect
        val doc = db.collection("chatrooms").document(chatroomId).get().await()
        val ownerId = doc.getString("ownerId")
        isOwner = ownerId == uid
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(chatroomName) },
                actions = {
                    if (isOwner) {
                        TextButton(onClick = onDelete) {
                            Text("Delete")
                        }
                    } else {
                        TextButton(onClick = onLeave) {
                            Text("Leave")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("You're in $chatroomName")
        }
    }
}
