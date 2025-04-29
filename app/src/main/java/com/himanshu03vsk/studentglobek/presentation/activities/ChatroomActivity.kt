package com.himanshu03vsk.studentglobek.presentation.activities

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.himanshu03vsk.studentglobek.domain.model.Message
import com.himanshu03vsk.studentglobek.domain.usecase.ChatService
import com.himanshu03vsk.studentglobek.domain.usecase.MediaUploadService
import com.himanshu03vsk.studentglobek.presentation.components.ChatBubble
import com.himanshu03vsk.studentglobek.presentation.components.MediaBubble
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
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val chatService = remember { ChatService() }
    val mediaUploadService = remember { MediaUploadService() }

    val uid = auth.currentUser?.uid ?: ""
    var name by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var inputMessage by remember { mutableStateOf("") }
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var uploadedMediaUrl by remember { mutableStateOf<String?>(null) }
    var isOwner by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }  // <-- Loading state added

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedMediaUri = uri
    }

    // Fetch user details
    LaunchedEffect(uid) {
        val doc = db.collection("users").document(uid).get().await()
        if (doc.exists()) {
            name = doc.getString("name") ?: "Unknown User"
        }
    }

    // Fetch chat history and connect to chat socket
    LaunchedEffect(chatroomId) {
        val doc = db.collection("chatrooms").document(chatroomId).get().await()
        isOwner = doc.getString("ownerId") == uid

        messages = fetchChatHistory(chatroomId)

        isLoading = false // <-- Set loading to false after fetching messages

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
            Column {
                if (selectedMediaUri != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(selectedMediaUri),
                            contentDescription = "Selected Media",
                            modifier = Modifier
                                .size(100.dp)
                                .padding(end = 8.dp),
                            contentScale = ContentScale.Crop
                        )
                        Button(onClick = { selectedMediaUri = null }) {
                            Text("Remove")
                        }
                    }
                }

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
                    IconButton(onClick = { launcher.launch("*/*") }) {
                        Icon(Icons.Default.Share, contentDescription = "Attach Media")
                    }
                    Button(
                        onClick = {
                            if (selectedMediaUri != null) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    mediaUploadService.uploadMediaToChatroom(
                                        chatroomId = chatroomId,
                                        mediaUri = selectedMediaUri!!,
                                        onMediaUploaded = { media ->
                                            uploadedMediaUrl = "https://chat-server-y96l.onrender.com${media.mediaUrl}"
                                            val msg = Message(
                                                chatroomId = chatroomId,
                                                senderId = uid,
                                                senderName = name,
                                                content = inputMessage.trim(),
                                                mediaUrl = uploadedMediaUrl
                                            )
                                            chatService.sendMessage(msg)
                                            inputMessage = ""
                                            selectedMediaUri = null
                                            uploadedMediaUrl = null
                                        },
                                        context = context
                                    )
                                }
                            } else if (inputMessage.isNotBlank()) {
                                val msg = Message(
                                    chatroomId = chatroomId,
                                    senderId = uid,
                                    senderName = name,
                                    content = inputMessage.trim()
                                )
                                chatService.sendMessage(msg)
                                inputMessage = ""
                            }
                        },
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text("Send")
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            // 👇 Show a centered Circular Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // 👇 Actual Chat List
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                items(messages) { message ->
                    if (!message.mediaUrl.isNullOrEmpty()) {
                        MediaBubble(
                            mediaUrl = message.mediaUrl,
                            senderName = message.senderName ?: "Unknown",
                            isCurrentUser = message.senderId == uid,
                            content = message.content.takeIf { !it.isNullOrBlank() }
                        )
                    } else {
                        message.content?.let {
                            ChatBubble(
                                message = it,
                                senderName = message.senderName ?: "Unknown",
                                isCurrentUser = message.senderId == uid
                            )
                        }
                    }
                }
            }
        }
    }
}


suspend fun fetchChatHistory(chatroomId: String): List<Message> {
    return try {
        val gson = Gson()

        // Fetch text messages
        val messagesUrl = URL("https://chat-server-y96l.onrender.com/api/messages/chatroom/$chatroomId")
        val messagesConnection = withContext(Dispatchers.IO) {
            messagesUrl.openConnection() as HttpURLConnection
        }
        messagesConnection.requestMethod = "GET"
        messagesConnection.connectTimeout = 10000
        messagesConnection.readTimeout = 10000

        val messagesResponse = withContext(Dispatchers.IO) {
            messagesConnection.inputStream.bufferedReader().use { it.readText() }
        }
        val messageType = object : TypeToken<List<Message>>() {}.type
        var messages: List<Message> = gson.fromJson(messagesResponse, messageType)

        // Fetch media messages
        val mediaUrl = URL("https://chat-server-y96l.onrender.com/api/media/chatroom/$chatroomId")
        val mediaConnection = withContext(Dispatchers.IO) {
            mediaUrl.openConnection() as HttpURLConnection
        }
        mediaConnection.requestMethod = "GET"
        mediaConnection.connectTimeout = 10000
        mediaConnection.readTimeout = 10000

        val mediaResponse = withContext(Dispatchers.IO) {
            mediaConnection.inputStream.bufferedReader().use { it.readText() }
        }
        val mediaType = object : TypeToken<List<Message>>() {}.type
        var mediaMessages: List<Message> = gson.fromJson(mediaResponse, mediaType)

        // Fix media URLs if needed
        messages = messages.map { message ->
            if (!message.mediaUrl.isNullOrEmpty() && !message.mediaUrl.startsWith("http")) {
                message.copy(mediaUrl = "https://chat-server-y96l.onrender.com${message.mediaUrl}")
            } else {
                message
            }
        }

        mediaMessages = mediaMessages.map { media ->
            if (!media.mediaUrl.isNullOrEmpty() && !media.mediaUrl.startsWith("http")) {
                media.copy(mediaUrl = "https://chat-server-y96l.onrender.com${media.mediaUrl}")
            } else {
                media
            }
        }

        // Merge both messages and media and sort by createdAt timestamp
        val allMessages = (messages + mediaMessages).sortedBy { it.timestamp }

        allMessages

    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}