package com.himanshu03vsk.studentglobek.presentation.components

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.himanshu03vsk.studentglobek.domain.model.Chatroom
import com.himanshu03vsk.studentglobek.presentation.activities.ChatroomActivity

@Composable
fun ChatroomCardCreated(chatroom: Chatroom) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .width(250.dp)
            .clickable {
                val intent = Intent(context, ChatroomActivity::class.java).apply {
                    putExtra("chatroomId", chatroom.id)
                    putExtra("chatroomName", chatroom.name)
                }
                context.startActivity(intent)
            },
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = chatroom.name, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Major: ${chatroom.major}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Department: ${chatroom.department}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Created by You", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}
