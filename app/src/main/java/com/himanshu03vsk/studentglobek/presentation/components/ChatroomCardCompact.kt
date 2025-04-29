package com.himanshu03vsk.studentglobek.presentation.components

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.*

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.himanshu03vsk.studentglobek.domain.model.Chatroom
import com.himanshu03vsk.studentglobek.presentation.activities.ChatroomActivity

@Composable
fun ChatroomCardCompact(chatroom: Chatroom) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .width(220.dp)
            .padding(4.dp)
            .animateContentSize() // Smooth size animation
            .clickable {
                val intent = Intent(context, ChatroomActivity::class.java).apply {
                    putExtra("chatroomId", chatroom.id)
                    putExtra("chatroomName", chatroom.chatroomName)
                }
                context.startActivity(intent)
            },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = chatroom.chatroomName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Major: ${chatroom.major}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
