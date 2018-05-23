package cz.strazovan

import cz.strazovan.models.ContentType
import cz.strazovan.models.Message
import cz.strazovan.models.MessageType
import cz.strazovan.models.User
import io.javalin.Javalin
import io.javalin.event.EventType

fun main(args: Array<String>) {

    val rooms = listOf<Room>(Room("KAJ"), Room("LAG"))
    val testRoom = Room("test1")
    val app = Javalin.create().port(7000)

    app.get("/testMessage"){
        ctx ->
        ctx.json(Message(User("Server"), ContentType.TEXT_MESSAGE, MessageType.MESSAGE, "Ahoj"))
    }
     // for each room create endpoint?

    rooms.forEach {
        app.ws("/rooms/${it.name}", it)
    }

    app.event(EventType.SERVER_STARTING) {
        println("starting")
    }

    app.event(EventType.SERVER_STOPPING) {
        println("stopping")
    }

    Runtime.getRuntime().addShutdownHook(Thread(){
        app.stop()
    })
    app.start()
}

