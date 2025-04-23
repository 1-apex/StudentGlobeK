package com.himanshu03vsk.studentglobek.domain.model

data class Message(
    val chatroomId: String = "",   // Links to Firestore's chatroom
    val senderId: String = "",
    val senderName: String = "",
    val content: String = "",
    val timestamp: String = ""
)
