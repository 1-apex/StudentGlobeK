package com.himanshu03vsk.studentglobek.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.himanshu03vsk.studentglobek.domain.model.Chatroom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SearchChatroomsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _chatrooms = MutableStateFlow<List<Chatroom>>(emptyList())
    val chatrooms: StateFlow<List<Chatroom>> = _chatrooms.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        fetchAllChatrooms()
    }

    private fun fetchAllChatrooms() {
        _isLoading.value = true
        val currentUserId = auth.currentUser?.uid ?: run {
            _error.value = "User not authenticated"
            _isLoading.value = false
            return
        }

        db.collection("chatrooms")
            .get()
            .addOnSuccessListener { snapshot ->
                val allChatrooms = snapshot.documents
                    .mapNotNull { it.toChatroom() }
                    .filter { chatroom ->
                        // Exclude chatrooms where the user is already a member
                        !chatroom.members.contains(currentUserId)
                    }

                _chatrooms.value = allChatrooms
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                _error.value = "Failed to fetch chatrooms: ${e.message}"
                _isLoading.value = false
            }
    }


    fun searchChatrooms(query: String) {
        val filtered = _chatrooms.value.filter {
            it.chatroomName.contains(query, ignoreCase = true)
        }
        _chatrooms.value = filtered
    }

    fun joinChatroom(chatroomId: String) {
        val uid = auth.currentUser?.uid ?: return

        val chatroomRef = db.collection("chatrooms").document(chatroomId)

        chatroomRef.update("members", FieldValue.arrayUnion(uid))
            .addOnSuccessListener {
                Log.d("JoinChatroom", "Successfully joined chatroom.")
                fetchAllChatrooms() // Refresh after joining
            }
            .addOnFailureListener {
                Log.e("JoinChatroom", "Failed to join chatroom.", it)
            }
    }

    private fun DocumentSnapshot.toChatroom(): Chatroom {
        @Suppress("UNCHECKED_CAST")
        return Chatroom(
            id = id,
            chatroomName = getString("chatroomName") ?: "",
            type = getString("chatroomType") ?: "Semester",
            ownerId = getString("ownerId") ?: "",
            members = get("members") as? List<String> ?: emptyList(),
            major = getString("major") ?: "",
            department = getString("department") ?: "",
            createdAt = getTimestamp("createdAt") ?: Timestamp.now(),
            expiry = getTimestamp("expiry") ?: Timestamp.now()
        )
    }
}
