package cz.strazovan

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import cz.strazovan.models.ContentType
import cz.strazovan.models.Message
import cz.strazovan.models.MessageType

import cz.strazovan.models.User
import io.javalin.embeddedserver.jetty.websocket.WebSocketConfig
import io.javalin.embeddedserver.jetty.websocket.WebSocketHandler

import org.eclipse.jetty.websocket.api.Session
import java.time.LocalDateTime
import java.util.*

import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList

class Room(val name: String,
           val users: MutableMap<Session, User> = ConcurrentHashMap(),
           val messages : MutableList<Message> = ArrayList()) : WebSocketConfig {

    private val waitingUsers = ArrayList<Session>()
    private val gson = GsonBuilder().create()
    private var lastMessageAt = LocalDateTime.now()

    override fun configure(ws: WebSocketHandler?) {

        ws?.onConnect {
            waitingUsers.add(it)
            println("user connected")
            it.policy.maxTextMessageSize = 1024 * 1024 * 10 // 10 MB
        }


        ws?.onMessage { session, msg ->

            val received = gson.fromJson<Message>(msg, Message::class.java)

            when (received.messageType) {

                MessageType.LOGIN -> {
                    val duplicate = users.values.find { it -> it.username == received.content }
                    if (duplicate == null) {
                        waitingUsers.remove(session)
                        val user = User(received.content)
                        users.put(session, user)
                        println("user logged in with name: ${received.content}")
                        val successMessage = Message(User("server"), ContentType.TEXT_MESSAGE, MessageType.LOGIN, "success")
                        session.send(gson.toJson(successMessage))
                        val newUserMessage = Message(User("server"), ContentType.USER_CONNECTED, MessageType.USERS, gson.toJson(user))
                        broadcast(gson.toJson(newUserMessage), session)
                    } else {
                        val loginFailedMessage = Message(User("server"), ContentType.TEXT_MESSAGE, MessageType.LOGIN, "Login failed")
                        session.send(gson.toJson(loginFailedMessage))
                    }

                }
                MessageType.MESSAGE -> {
                    val time = LocalDateTime.now()
                    messages.add(received)
                    if(time.isAfter(lastMessageAt))
                        lastMessageAt = time
                    broadcast(msg)
                }
                MessageType.COMMAND -> {
                    val split = received.content.split(" ")
                    val cmd = split[0]
                    when(cmd){
                        "getHistory" -> {
                            val relevantHistory = messages.takeLast(50)
                            val message = Message(User("server"), ContentType.MESSAGE_LIST, MessageType.MESSAGE, gson.toJson(relevantHistory) )
                            session.send(gson.toJson(message))
                        }

                        "getUsers" -> {

                            val message = Message(User("server"), ContentType.USERS_LIST, MessageType.USERS, gson.toJson(users.values))
                            session.send(gson.toJson(message))
                        }

                    }
                }
            }
        }

        ws?.onClose { session, statusCode, reason ->
            val user = users[session]
            println("user $user disconnected")
            users.remove(session)
            val newUserMessage = Message(User("server"), ContentType.USER_DISCONNECTED, MessageType.USERS, gson.toJson(user))
            broadcast(gson.toJson(newUserMessage), session)
        }
    }

    private fun broadcast(msg: String?, skip : Session? = null) {
        users.forEach { key, value ->

            if (key.isOpen && (skip == null || skip != key))
                key.remote.sendString(msg)
        }
    }


}