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

    // Fetch registered events (one-time)
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

    // Real-time updates for created events
    LaunchedEffect(uid) {
        FirebaseFirestore.getInstance()
            .collection("events")
            .whereEqualTo("ownerId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                createdEvents.clear()
                createdEvents.addAll(
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Event::class.java)?.copy(eventId = doc.id)
                    }
                )
            }
    }

    // Real-time updates for created chatrooms
    LaunchedEffect(uid) {
        FirebaseFirestore.getInstance()
            .collection("chatrooms")
            .whereEqualTo("ownerId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                createdChatrooms.clear()
                createdChatrooms.addAll(
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Chatroom::class.java)?.copy(id = doc.id)
                    }
                )
            }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerMenu { label, destination ->
                val activity = context as? Activity
                activity?.startActivity(Intent(context, destination))
                scope.launch { drawerState.close() }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Dashboard") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Joined Chatrooms
                item {
                    Text("Joined Chatrooms", style = MaterialTheme.typography.titleMedium)
                    if (isLoading) CircularProgressIndicator()
                    else if (chatrooms.isEmpty()) Text("No joined chatrooms.")
                    else LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(chatrooms) { ChatroomCardCompact(it) }
                    }
                }

                // Registered Events
                item { Text("Registered Events", style = MaterialTheme.typography.titleMedium) }
                items(registeredEvents) { EventCard(event = it) }

                // Created Chatrooms
                item { Text("Created Chatrooms", style = MaterialTheme.typography.titleMedium) }
                if (createdChatrooms.isEmpty()) {
                    item { Text("No created chatrooms.") }
                } else {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(createdChatrooms) { ChatroomCardCreated(it) }
                        }
                    }
                }

                // Created Events
                item { Text("Created Events", style = MaterialTheme.typography.titleMedium) }
                if (createdEvents.isEmpty()) {
                    item { Text("No created events.") }
                } else {
                    items(createdEvents) { EventCardCreated(event = it) }
                }
            }
        }
    }
}

@SuppressLint("ContextCastToActivity")
@Composable
fun DrawerMenu(onItemClick: (String, Class<*>) -> Unit) {
    val activity = LocalContext.current as? Activity
    val navItems = listOf(
//        "Landing Page" to LandingActivity::class.java,
        "Create Chatroom" to CreateChatroomActivity::class.java,
        "Create Event" to CreateEventActivity::class.java,
//        "Joined Chatrooms" to JoinedChatroomsActivity::class.java,
//        "User Profile" to EditProfileActivity::class.java,
//        "Home Page" to HomePageActivity::class.java,
//        "Chatroom" to ChatroomActivity::class.java,
//        "Login" to LoginActivity::class.java,
        "Search Peers" to SearchPeersActivity::class.java,
//        "Sign Up" to SignUpActivity::class.java,
//        "Verify Account" to VerifyAccountActivity::class.java,
        "View Chatrooms" to ViewChatroomsActivity::class.java,
        "View Events" to ViewEventsActivity::class.java
    )

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(WindowInsets.systemBars.asPaddingValues())
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Menu", style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
        )
        navItems.forEach { (label, destination) ->
            NavigationDrawerItem(
                label = { Text(label) },
                selected = false,
                onClick = { onItemClick(label, destination) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        NavigationDrawerItem(
            label = { Text("Logout") },
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
