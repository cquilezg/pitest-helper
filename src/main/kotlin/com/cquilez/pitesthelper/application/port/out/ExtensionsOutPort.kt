package com.cquilez.pitesthelper.application.port.out

import com.cquilez.pitesthelper.application.port.BuildSystemProcessor
import com.cquilez.pitesthelper.application.port.LanguageProcessor

interface ExtensionsOutPort {
    fun <T> getMavenExtension(): BuildSystemProcessor<T>

    fun <T> getGradleExtension(): BuildSystemProcessor<T>

    fun getKotlinExtension(): LanguageProcessor
}