package com.himanshu03vsk.studentglobek.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ChatBubble(
    message: String,
    sender: String,
    isCurrentUser: Boolean
) {
    val alignment = if (isCurrentUser) Alignment.End else Alignment.Start
    val backgroundColor = if (isCurrentUser) Color(0xFFDCF8C6) else Color(0xFFFFFFFF)
    val textColor = if (isCurrentUser) Color.Black else Color.Gray

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
                .align(alignment)
        ) {
            Text(
                text = sender,
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

@Preview
@Composable
fun PreviewChatBubble() {
    ChatBubble(message = "Hello, this is a message!", sender = "John Doe", isCurrentUser = true)
}
