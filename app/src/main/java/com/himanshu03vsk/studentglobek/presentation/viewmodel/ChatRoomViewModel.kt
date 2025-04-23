package com.himanshu03vsk.studentglobek.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.himanshu03vsk.studentglobek.domain.model.Chatroom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatRoomsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var snapshotListener: ListenerRegistration? = null

    private val _chatrooms = MutableStateFlow<List<Chatroom>>(emptyList())
    val chatrooms: StateFlow<List<Chatroom>> = _chatrooms.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        setupFirestoreListener()
    }

    private fun setupFirestoreListener() {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid ?: run {
                    _error.value = "User not authenticated"
                    return@launch
                }

                snapshotListener = db.collection("chatrooms")
                    .addSnapshotListener { snapshot, error ->
                        _isLoading.value = false

                        if (error != null) {
                            _error.value = "Error fetching chatrooms: ${error.message}"
                            return@addSnapshotListener
                        }

                        val chatroomList = mutableListOf<Chatroom>()
                        snapshot?.documents?.forEach { doc ->

                            val members = doc.get("members") as List<*>

                            // Check if currentUserId is NOT in the members array
                            if (!members.contains(currentUserId)) {
                                chatroomList.add(doc.toChatroom())
                            }
                        }
                        _chatrooms.value = chatroomList
                    }
            } catch (e: Exception) {
                _error.value = "Error initializing listener: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    private fun DocumentSnapshot.toChatroom(): Chatroom {
        @Suppress("UNCHECKED_CAST")

        return Chatroom(
            id = id,
            name = getString("chatroomName") ?: "",
            type = getString("chatroomType") ?: "Semester",
            ownerId = getString("ownerId") ?: "",
            members = get("members") as? List<String> ?: emptyList(),
            major = getString("major") ?: "",
            department = getString("department") ?: "",
            createdAt = getTimestamp("createdAt") ?: Timestamp.now(),
            expiry = getTimestamp("expiry") ?: Timestamp.now()
        )
    }

    override fun onCleared() {
        super.onCleared()
        snapshotListener?.remove()
    }
}
