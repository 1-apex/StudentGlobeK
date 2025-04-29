package com.himanshu03vsk.studentglobek.domain.model

import com.google.firebase.Timestamp

data class Chatroom(
    val id: String = "",
    val chatroomName: String = "",
    val type: String = "Semester",
    val ownerId: String = "",
    val members: List<String> = emptyList(),
//    val membersSize: Int = members.size,
    val major: String = "",
    val department: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val expiry: Timestamp = Timestamp.now()
)
