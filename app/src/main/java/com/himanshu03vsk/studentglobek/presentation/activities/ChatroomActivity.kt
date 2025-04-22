package com.himanshu03vsk.studentglobek.presentation.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.himanshu03vsk.studentglobek.domain.model.Message
import com.himanshu03vsk.studentglobek.domain.usecase.ChatService
import com.himanshu03vsk.studentglobek.ui.components.ChatBubble
import com.himanshu03vsk.studentglobek.ui.theme.StudentGlobeKTheme
import kotlinx.coroutines.Dispatchers
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
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid ?: ""
    var isOwner by remember { mutableStateOf(false) }
    val chatService = remember { ChatService() }

    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var inputMessage by remember { mutableStateOf("") }

    LaunchedEffect(chatroomId) {
        // Check ownership
        val doc = db.collection("chatrooms").document(chatroomId).get().await()
        isOwner = doc.getString("ownerId") == uid

        // Fetch chat history from backend
        messages = fetchChatHistory(chatroomId)

        // Listen to incoming messages via socket
        chatService.connectToChatServer(chatroomId) { incomingMessage ->
            // Only update the message list when a new message arrives
            messages = messages + incomingMessage
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            chatService.disconnect()
        }
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
                Button(
                    onClick = {
                        if (inputMessage.isNotBlank()) {
                            val msg = Message(
                                senderId = uid,
                                chatroomId = chatroomId,
                                content = inputMessage.trim()
                            )
                            // Send the message via socket, but don't add it here manually
                            chatService.sendMessage(msg)
                            inputMessage = ""  // Clear the input field
                        }
                    },
                    modifier = Modifier.padding(start = 8.dp)
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
