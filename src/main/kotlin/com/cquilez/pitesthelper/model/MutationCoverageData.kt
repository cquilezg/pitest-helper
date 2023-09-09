package com.cquilez.pitesthelper.model

import com.intellij.openapi.module.Module

class MutationCoverageData (val module: Module, val targetClasses: List<String>, val targetTests: List<String>)