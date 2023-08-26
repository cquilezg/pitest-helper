package com.cquilez.pitesthelper.model

import com.intellij.openapi.module.Module

class MutationCoverageData (val module: Module, val targetClasses: String, val targetTests: String)