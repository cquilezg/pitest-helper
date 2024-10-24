package com.cquilez.pitesthelper.infrastructure.dto

import com.cquilez.pitesthelper.domain.ProjectElement
import com.intellij.openapi.externalSystem.model.project.ContentRootData.SourceRoot
import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile

class ProjectElementDataDTO(
    val projectElement: ProjectElement,
    val module: Module,
    val virtualFile: VirtualFile,
    val sourceRoot: SourceRoot
)