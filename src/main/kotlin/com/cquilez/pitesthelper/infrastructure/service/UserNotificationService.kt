package com.cquilez.pitesthelper.infrastructure.service

import com.cquilez.pitesthelper.application.port.out.UserNotificationOutPort
import com.intellij.openapi.components.Service
import com.intellij.openapi.ui.Messages

@Service
class UserNotificationService : UserNotificationOutPort {
    override fun notifyUser(title: String, message: String) {
        Messages.showInfoMessage(message, title)
    }
}
