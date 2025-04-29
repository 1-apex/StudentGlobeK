package com.himanshu03vsk.studentglobek.domain.model

data class Message(
    val chatroomId: String = "",   // Links to Firestore's chatroom
    val senderId: String = "",
    val senderName: String?= null,
    val timestamp: String = "",
    val content: String?, // for text
    val mediaUrl: String? = null // for media
)
