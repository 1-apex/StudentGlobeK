package com.himanshu03vsk.studentglobek.domain.usecase

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.himanshu03vsk.studentglobek.domain.model.Media
import com.himanshu03vsk.studentglobek.domain.model.Chatroom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.*
import java.io.File
import java.io.IOException

class MediaUploadService {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun uploadMediaToChatroom(
        chatroomId: String,
        mediaUri: Uri,
        onMediaUploaded: (Media) -> Unit
    ) {
        try {
            // Step 1: Get chatroom details from Firestore
            val chatroomDetails = getChatroomDetails(chatroomId)
            val userId = auth.currentUser?.uid ?: return

            // Step 2: Create the file object from URI (e.g., image or pdf)
            val file = File(mediaUri.path!!)

            // Prepare the request body for the file upload (multipart)
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, RequestBody.create(MediaType.parse("application/octet-stream"), file))
                .addFormDataPart("chatroomId", chatroomId)
                .addFormDataPart("senderId", userId)
                .build()

            val client = OkHttpClient()

            val request = Request.Builder()
                .url("http://192.168.56.1:5000/upload") // Replace with your actual backend URL
//                .url("https://chat-server-y96l.onrender.com/upload")
                .post(requestBody)
                .build()

            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }

            if (response.isSuccessful) {
                val media = Media(
                    chatroomId = chatroomId,
                    senderId = userId,
                    mediaUrl = "/file/${file.name}" // Assuming the file is stored in GridFS and accessible via this URL
                )
                onMediaUploaded(media)
            } else {
                throw IOException("File upload failed: ${response.message()}")
            }

        } catch (e: Exception) {
            Log.e("MediaUploadService", "Error uploading media: ${e.message}")
        }
    }

    // Fetch chatroom details
    private suspend fun getChatroomDetails(chatroomId: String): Chatroom? {
        return try {
            val document = db.collection("chatrooms").document(chatroomId).get().await()
            document.toObject(Chatroom::class.java)
        } catch (e: Exception) {
            Log.e("MediaUploadService", "Error fetching chatroom details: ${e.message}")
            null
        }
    }
}
