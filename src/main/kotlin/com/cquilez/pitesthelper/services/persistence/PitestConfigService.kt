package com.cquilez.pitesthelper.services.persistence

import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.State

@Service(Service.Level.PROJECT)
@State(name = "project.mutationcoverage", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class PitestConfigService : SerializablePersistentStateComponent<PitestConfigService.State>(State()) {

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

  data class State (
    @JvmField val preGoals: String = "",
    @JvmField val postGoals: String = ""
  )
}