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
import java.io.IOException
import okio.BufferedSink
import okio.source
import org.json.JSONObject
import android.content.Context
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class MediaUploadService {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // This is now a standard suspend function
    suspend fun uploadMediaToChatroom(
        context: Context,
        chatroomId: String,
        mediaUri: Uri,
        onMediaUploaded: (Media) -> Unit
    ) {
        try {
            val userId = auth.currentUser?.uid ?: return

            // Get content resolver from passed context
            val contentResolver = context.contentResolver

            val inputStream = contentResolver.openInputStream(mediaUri)
            val fileName = "upload_${System.currentTimeMillis()}" // Or use metadata to get real filename

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file", fileName,
                    object : RequestBody() {
                        override fun contentType(): MediaType? =
                            "application/octet-stream".toMediaTypeOrNull()

                        override fun writeTo(sink: BufferedSink) {
                            inputStream?.use { input ->
                                sink.writeAll(input.source())
                            }
                        }
                    }
                )
                .addFormDataPart("chatroomId", chatroomId)
                .addFormDataPart("senderId", userId)
                .build()

            val request = Request.Builder()
                .url("https://chat-server-y96l.onrender.com/upload")
                .post(requestBody)
                .build()

            val client = OkHttpClient()
            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val json = responseBody?.let { JSONObject(it) }
                val mediaJson = json?.getJSONObject("media")

                val media = mediaJson?.let {
                    Media(
                        chatroomId = it.getString("chatroomId"),
                        senderId = it.getString("senderId"),
                        mediaUrl = it.getString("mediaUrl")
                    )
                }

                media?.let {
                    onMediaUploaded(it)
                }
            } else {
                Log.e(
                    "MediaUploadService",
                    "Upload failed: ${response.code} ${response.message}"
                )
            }

        } catch (e: Exception) {
            Log.e("MediaUploadService", "Error uploading media: ${e.message}")
        }
    }
}
