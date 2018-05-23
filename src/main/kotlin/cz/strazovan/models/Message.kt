package cz.strazovan.models


enum class ContentType(val value : String){
    TEXT_MESSAGE("message/text"), IMAGE("message/image"), MESSAGE_LIST("message/list"),

    POSITION("message/position"),

    USERS_LIST("message/users-list"),

    USER_CONNECTED("message/user-connected"),

    USER_DISCONNECTED("message/user-disconnected")

}
enum class MessageType(val value : String) {
    LOGIN("login"), MESSAGE("message"), COMMAND("cmd"),

    USERS("users")
}

data class Message(val sender : User,
                   val contentType : ContentType,
                   val messageType : MessageType,
                   val content : String) {
}