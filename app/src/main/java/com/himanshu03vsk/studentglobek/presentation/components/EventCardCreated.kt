package com.himanshu03vsk.studentglobek.presentation.components

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.himanshu03vsk.studentglobek.domain.model.Event
import com.himanshu03vsk.studentglobek.presentation.activities.EventActivity

@Composable
fun EventCardCreated(event: Event) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                val intent = Intent(context, EventActivity::class.java).apply {
                    putExtra("eventId", event.eventId)
                    putExtra("eventName", event.eventName)
                    putExtra("description", event.description)
                    putExtra("startDate", event.startDate)
                    putExtra("endDate", event.endDate)
                    putExtra("major", event.major)
                    putExtra("department", event.department)
                    putExtra("ownerId", event.ownerId)
                }
                context.startActivity(intent)
            },
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = event.eventName, style = MaterialTheme.typography.titleMedium)
            Text(text = "Major: ${event.major}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Department: ${event.department}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Created by You", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}
