// Event.kt
package com.himanshu03vsk.studentglobek.domain.model

data class Event(
    val eventId: String = "",
    val eventName: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val major: String = "",
    val department: String = "",
    val description: String = "",
    val ownerId: String = "",
//    val createdAt: String = "",
    val registeredUsers: List<String> = emptyList()
)
