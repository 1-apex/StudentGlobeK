package com.himanshu03vsk.studentglobek.domain.usecase



//class ChatService {
//
//    private lateinit var socket: Socket
//    private val gson = Gson()
//
//    fun connectToChatServer(chatroomId: String, onMessageReceived: (Message) -> Unit) {
//        try {
//            val options = IO.Options()
//            socket = IO.socket("https://chat-server-y96l.onrender.com", options)
////            socket = IO.socket("http://192.168.56.1:5000", options)
////            socket = IO.socket("http://10.0.2.2:5000", options)
//
//
//            socket.connect()
//
//            socket.on(Socket.EVENT_CONNECT) {
//                Log.d("Socket", "Connected to server")
//                socket.emit("join_room", chatroomId)
//            }
//
//            socket.on("send_message", Emitter.Listener { args ->
//                if (args.isNotEmpty()) {
//                    val data = args[0] as JSONObject
//                    val message = gson.fromJson(data.toString(), Message::class.java)
//                    onMessageReceived(message)
//                }
//            })
//
//        } catch (e: Exception) {
//            Log.e("SocketError", "Error: ${e.message}")
//        }
//    }
//
//    fun sendMessage(message: Message) {
//        val json = gson.toJson(message)
//        socket.emit("send_message", JSONObject(json))
//    }
//
//    fun disconnect() {
//        socket.disconnect()
//    }
//}




import android.util.Log
import com.google.gson.Gson
import com.himanshu03vsk.studentglobek.domain.model.Message
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject

class ChatService {

    private lateinit var socket: Socket
    private val gson = Gson()

    fun connectToChatServer(chatroomId: String, onMessageReceived: (Message) -> Unit) {
        try {
            if (!::socket.isInitialized || !socket.connected()) {
                val options = IO.Options()
                socket = IO.socket("https://chat-server-y96l.onrender.com", options)
                socket.connect()

                socket.on(Socket.EVENT_CONNECT) {
                    Log.d("Socket", "Connected to server")
                    socket.emit("join_room", chatroomId)
                }

                // Listen for incoming messages
                socket.on("receive_message") { args ->
                    if (args.isNotEmpty()) {
                        val data = args[0] as JSONObject
                        val message = gson.fromJson(data.toString(), Message::class.java)
                        onMessageReceived(message)
                    }
                }

                // Handle disconnection
                socket.on(Socket.EVENT_DISCONNECT) {
                    Log.d("Socket", "Disconnected from server")
                }

            } else {
                Log.d("Socket", "Socket already connected")
            }
        } catch (e: Exception) {
            Log.e("SocketError", "Error: ${e.message}")
        }
    }

    fun sendMessage(message: Message) {
        try {
            val json = gson.toJson(message)
            socket.emit("send_message", JSONObject(json))
        } catch (e: Exception) {
            Log.e("SocketError", "Error while sending message: ${e.message}")
        }
    }

    fun disconnect() {
        try {
            if (::socket.isInitialized && socket.connected()) {
                socket.disconnect()
            }
        } catch (e: Exception) {
            Log.e("SocketError", "Error during disconnect: ${e.message}")
        }
    }
}
