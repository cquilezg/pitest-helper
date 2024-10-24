package com.cquilez.pitesthelper.infrastructure.service

import com.cquilez.pitesthelper.application.port.`in`.ActionVisibilityInPort
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.Service

@Service
class ActionVisibilityService : ActionVisibilityInPort<AnActionEvent> {

    // TODO: only has to be visible if all elements are in modules with source files
    override fun isProjectViewActionVisible(inputData: AnActionEvent): Boolean {
        return inputData.project != null && inputData.getData(CommonDataKeys.NAVIGATABLE_ARRAY) != null
    }

    override fun isEditorActionVisible(inputData: AnActionEvent): Boolean {
        return inputData.project != null && inputData.getData(CommonDataKeys.EDITOR) != null
    }
}
