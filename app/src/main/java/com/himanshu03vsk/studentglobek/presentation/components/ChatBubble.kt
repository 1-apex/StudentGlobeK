package com.himanshu03vsk.studentglobek.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun ChatBubble(
    message: String,
    senderName: String,
    isCurrentUser: Boolean
) {
    val alignment = if (isCurrentUser) Alignment.End else Alignment.Start
    val backgroundColor = if (isCurrentUser) Color(0xFFDCF8C6) else Color(0xFFFFFFFF)
    val textColor = if (isCurrentUser) Color.Black else Color.Gray

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = alignment
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = senderName,
                    style = TextStyle(fontSize = 12.sp, color = textColor),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = message,
                    style = TextStyle(fontSize = 16.sp, color = textColor)
                )
            }
        }
    }
}
