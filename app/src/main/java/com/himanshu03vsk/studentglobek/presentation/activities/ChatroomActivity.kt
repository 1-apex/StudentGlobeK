package com.himanshu03vsk.studentglobek.presentation.activities

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.himanshu03vsk.studentglobek.domain.model.Message
import com.himanshu03vsk.studentglobek.domain.usecase.ChatService
import com.himanshu03vsk.studentglobek.domain.usecase.MediaUploadService
import com.himanshu03vsk.studentglobek.ui.components.ChatBubble
import com.himanshu03vsk.studentglobek.ui.theme.StudentGlobeKTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

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

    var isOwner by remember { mutableStateOf(false) }
    val chatService = remember { ChatService() }
    val mediaUploadService = remember { MediaUploadService() }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // Get the current user's UID (userID from Firebase Authentication)
    val uid = auth.currentUser?.uid ?: ""

    var name by remember { mutableStateOf("") } // Variable to hold the name

    // Fetch the user's details from Firestore
    val userRef = db.collection("users").document(uid)

    userRef.get().addOnSuccessListener { document ->
        if (document.exists()) {
            // Retrieve the user's data from the Firestore document
            name = document.getString("name") ?: "Unknown User"
            val email = document.getString("email") ?: "Unknown Email"
            val department = document.getString("department") ?: "Unknown Department"
            val major = document.getString("major") ?: "Unknown Major"
            val phNumber = document.getString("phNumber") ?: "Unknown Phone Number"

            // You can now use these values for other purposes if needed
            Log.d("User Info", "Name: $name, Email: $email, Department: $department, Major: $major, Phone: $phNumber")
        } else {
            // Document doesn't exist, handle the case
            Log.d("User Info", "No user found with this UID.")
        }
    }.addOnFailureListener { exception ->
        // Handle error, failed to fetch data from Firestore
        Log.d("User Info", "Error getting user data: ${exception.message}")
    }

    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var inputMessage by remember { mutableStateOf("") }

    // Media picker launcher (must be placed at composable scope)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            Toast.makeText(context, "Uploading media...", Toast.LENGTH_SHORT).show()
            CoroutineScope(Dispatchers.IO).launch {
                mediaUploadService.uploadMediaToChatroom(
                    chatroomId = chatroomId,
                    mediaUri = uri,
                    onMediaUploaded = { media ->
                        // Handle media upload callback here if needed
                    }
                )
            }
        }
    }

    LaunchedEffect(chatroomId) {
        // Check ownership
        val doc = db.collection("chatrooms").document(chatroomId).get().await()
        isOwner = doc.getString("ownerId") == uid

        // Fetch chat history
        messages = fetchChatHistory(chatroomId)

        // Connect to chat socket
        chatService.connectToChatServer(chatroomId) { incomingMessage ->
            messages = messages + incomingMessage
        }
    }

    DisposableEffect(Unit) {
        onDispose { chatService.disconnect() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(chatroomName) },
                actions = {
                    if (isOwner) {
                        TextButton(onClick = onDelete) { Text("Delete") }
                    } else {
                        TextButton(onClick = onLeave) { Text("Leave") }
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputMessage,
                    onValueChange = { inputMessage = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") }
                )
                IconButton(
                    onClick = {
                        launcher.launch("*/*") // Launch file picker
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Attach Media"
                    )
                }
                Button(
                    onClick = {
                        if (inputMessage.isNotBlank()) {
                            val msg = Message(
                                senderId = uid,
                                senderName = name,  // Use the fetched name here
                                chatroomId = chatroomId,
                                content = inputMessage.trim()
                            )
                            chatService.sendMessage(msg)
                            inputMessage = "" // Clear the input field after sending
                        }
                    },
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text("Send")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            items(messages) { message ->
                ChatBubble(
                    message = message.content,
                    sender = message.senderId,
                    senderName = message.senderName,
                    isCurrentUser = message.senderId == uid
                )
            }
        }
    }
}


suspend fun fetchChatHistory(chatroomId: String): List<Message> {
    return try {
//        val url = URL("http://10.0.2.2:5000/api/messages/chatroom/$chatroomId")
        val url = URL("https://chat-server-y96l.onrender.com/api/messages/chatroom/$chatroomId")
        val connection = withContext(Dispatchers.IO) {
            url.openConnection() as HttpURLConnection
        }

        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        val response = withContext(Dispatchers.IO) {
            connection.inputStream.bufferedReader().use { it.readText() }
        }

        val type = object : TypeToken<List<Message>>() {}.type
        Gson().fromJson(response, type)
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}
