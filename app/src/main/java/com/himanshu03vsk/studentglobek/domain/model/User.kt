package com.himanshu03vsk.studentglobek.domain.model

data class User(
    val userID: String = "",
    val userName: String = "",
    val name: String = "",
    val email: String = "",
    val major: String = "",
    val department: String = "",
    val phNumber: String = "",
    val nameLowerCase: String = name.lowercase()
)
