package com.cquilez.pitesthelper.application.port.out

interface UserNotificationOutPort {
    fun notifyUser(title: String, message: String)
}