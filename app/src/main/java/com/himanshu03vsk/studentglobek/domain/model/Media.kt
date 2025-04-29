package com.himanshu03vsk.studentglobek.domain.model

data class Media(
    val chatroomId: String,
    val senderId: String,
    val mediaUrl: String,  // e.g., "/file/1713801287392-myImage.jpg"
    val timestamp: String? = null  // if you're storing timestamps
)
