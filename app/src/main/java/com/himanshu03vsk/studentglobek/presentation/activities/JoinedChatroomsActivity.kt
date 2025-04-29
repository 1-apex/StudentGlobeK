package com.himanshu03vsk.studentglobek.presentation.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.himanshu03vsk.studentglobek.domain.model.Chatroom
import com.himanshu03vsk.studentglobek.domain.model.Event
import com.himanshu03vsk.studentglobek.presentation.components.*
import com.himanshu03vsk.studentglobek.presentation.viewmodel.JoinedChatRoomsViewModel
import com.himanshu03vsk.studentglobek.ui.theme.StudentGlobeKTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class JoinedChatroomsActivity : ComponentActivity() {
    private val viewModel: JoinedChatRoomsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StudentGlobeKTheme {
                DashboardScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: JoinedChatRoomsViewModel) {
    val context = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val chatrooms by viewModel.chatrooms.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val createdEvents = remember { mutableStateListOf<Event>() }
    val registeredEvents = remember { mutableStateListOf<Event>() }
    val createdChatrooms = remember { mutableStateListOf<Chatroom>() }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Fetching registered events
    LaunchedEffect(uid) {
        val snapshot = FirebaseFirestore.getInstance().collection("events").get().await()
        registeredEvents.clear()
        registeredEvents.addAll(
            snapshot.documents.mapNotNull { doc ->
                val event = doc.toObject(Event::class.java)
                val registeredUsers = doc.get("registeredUsers") as? List<*> ?: emptyList<Any>()
                event?.takeIf { uid in registeredUsers }?.copy(eventId = doc.id)
            }
        )
    }

    // Live listening for created events & chatrooms
    LaunchedEffect(uid) {
        FirebaseFirestore.getInstance()
            .collection("events")
            .whereEqualTo("ownerId", uid)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    createdEvents.clear()
                    createdEvents.addAll(it.documents.mapNotNull { d -> d.toObject(Event::class.java)?.copy(eventId = d.id) })
                }
            }
    }
    LaunchedEffect(uid) {
        FirebaseFirestore.getInstance()
            .collection("chatrooms")
            .whereEqualTo("ownerId", uid)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    createdChatrooms.clear()
                    createdChatrooms.addAll(it.documents.mapNotNull { d -> d.toObject(Chatroom::class.java)?.copy(id = d.id) })
                }
            }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerMenu { label, destination ->
                (context as? Activity)?.startActivity(Intent(context, destination))
                scope.launch { drawerState.close() }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("StudentGlobe Dashboard", style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                item {
                    SectionTitle("Joined Chatrooms")
                    if (isLoading) {
                        CenteredLoader()
                    } else if (chatrooms.isEmpty()) {
                        EmptyStateMessage("No chatrooms joined yet.")
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(chatrooms) { ChatroomCardCompact(it) }
                        }
                    }
                }

                item {
                    SectionTitle("Registered Events")
                    if (registeredEvents.isEmpty()) {
                        EmptyStateMessage("No registered events yet.")
                    }
                }
                items(registeredEvents) { event ->
                    EventCard(event)
                }

                item {
                    SectionTitle("Created Chatrooms")
                    if (createdChatrooms.isEmpty()) {
                        EmptyStateMessage("No chatrooms created yet.")
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(createdChatrooms) { ChatroomCardCreated(it) }
                        }
                    }
                }

                item {
                    SectionTitle("Created Events")
                    if (createdEvents.isEmpty()) {
                        EmptyStateMessage("No events created yet.")
                    }
                }
                items(createdEvents) { event ->
                    EventCardCreated(event)
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun CenteredLoader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun EmptyStateMessage(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@SuppressLint("ContextCastToActivity")
@Composable
fun DrawerMenu(onItemClick: (String, Class<*>) -> Unit) {
    val activity = LocalContext.current as? Activity
    val navItems = listOf(
        "Create Chatroom" to CreateChatroomActivity::class.java,
        "Create Event" to CreateEventActivity::class.java,
        "Search Peers" to SearchPeersActivity::class.java,
        "View Chatrooms" to ViewChatroomsActivity::class.java,
        "View Events" to ViewEventsActivity::class.java
    )

    ModalDrawerSheet {
        Spacer(Modifier.height(24.dp))
        Text(
            "Navigation",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )
        HorizontalDivider()

        navItems.forEach { (label, destination) ->
            NavigationDrawerItem(
                label = { Text(label) },
                selected = false,
                onClick = { onItemClick(label, destination) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        NavigationDrawerItem(
            label = { Text("Logout", color = MaterialTheme.colorScheme.error) },
            selected = false,
            onClick = {
                FirebaseAuth.getInstance().signOut()
                activity?.startActivity(Intent(activity, LandingActivity::class.java))
                activity?.finish()
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }
}

