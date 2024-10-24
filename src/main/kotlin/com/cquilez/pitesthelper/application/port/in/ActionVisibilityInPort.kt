package com.cquilez.pitesthelper.application.port.`in`

/**
 * Enables or disables action visibility
 */
interface ActionVisibilityInPort<T> {
    /**
     * If action is visible
     */
    fun isProjectViewActionVisible(inputData: T): Boolean

    fun isEditorActionVisible(inputData: T): Boolean
}