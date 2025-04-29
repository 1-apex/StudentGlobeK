package com.himanshu03vsk.studentglobek.presentation.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.himanshu03vsk.studentglobek.domain.model.User
import com.himanshu03vsk.studentglobek.presentation.viewmodel.SearchPeersViewModel
import com.himanshu03vsk.studentglobek.ui.theme.StudentGlobeKTheme

class SearchPeersActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StudentGlobeKTheme {
                SearchPeersScreen()
            }
        }
    }
}

@Composable
fun SearchPeersScreen(viewModel: SearchPeersViewModel = viewModel()) {
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getAllUsers()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                if (it.isNotBlank()) {
                    viewModel.searchPeers(it)
                } else {
                    viewModel.getAllUsers()
                }
            },
            label = { Text("Search Peers by Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (searchQuery.isNotEmpty() && searchResults.isEmpty()) {
            Text("No users found.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(searchResults) { user ->
                    PeerListItem(user)
                }
            }
        }
    }
}

@Composable
fun PeerListItem(user: User) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor)
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "@${user.userName}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = primaryColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = user.name, style = MaterialTheme.typography.bodyLarge, color = onPrimaryContainer)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = user.email, style = MaterialTheme.typography.bodyMedium, color = onPrimaryContainer)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = "Major: ${user.major}", style = MaterialTheme.typography.bodySmall, color = onPrimaryContainer)
            Text(text = "Department: ${user.department}", style = MaterialTheme.typography.bodySmall, color = onPrimaryContainer)
        }
    }
}
