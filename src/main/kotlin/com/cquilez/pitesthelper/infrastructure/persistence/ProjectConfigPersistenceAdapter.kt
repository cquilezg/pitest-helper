package com.cquilez.pitesthelper.infrastructure.persistence

import com.cquilez.pitesthelper.application.port.out.ProjectConfigPort
import com.cquilez.pitesthelper.domain.MutationCoverageProjectSettings
import com.intellij.openapi.components.*

@State(name = "project.mutationcoverage", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class ProjectConfigPersistenceAdapter : SerializablePersistentStateComponent<ProjectConfigPersistenceAdapter.State>(State()),
    ProjectConfigPort {

    var preGoals: String
        get() = state.preGoals
        set(value) {
            updateState {
                it.copy(preGoals = value)
            }
        }

    var postGoals: String
        get() = state.postGoals
        set(value) {
            updateState {
                it.copy(postGoals = value)
            }
        }

    data class State(
        @JvmField val preGoals: String = "",
        @JvmField val postGoals: String = ""
    )

    override fun getDefaultSettings(): MutationCoverageProjectSettings {
        return MutationCoverageProjectSettings(preGoals, postGoals)
    }
}