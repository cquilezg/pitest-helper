package com.cquilez.pitesthelper.services

import com.cquilez.pitesthelper.domain.exception.PitestHelperException
import com.cquilez.pitesthelper.extensions.KotlinProcessor
import com.cquilez.pitesthelper.extensions.LanguageProcessor
import com.intellij.openapi.components.Service
import com.intellij.openapi.extensions.ExtensionPointName

@Service
class LanguageProcessorService {

    companion object {
        private val LANGUAGE_PROCESSORS =
            ExtensionPointName.create<LanguageProcessor>("com.cquilez.pitesthelper.languageProcessor")
    }

    fun getKotlinExtension(): KotlinProcessor {
        val languageProcessor = LANGUAGE_PROCESSORS.extensionList.firstOrNull { it is KotlinProcessor }
        if (languageProcessor == null) {
            throw PitestHelperException("You need to enable Kotlin plugin to work with Kotlin classes.")
        }
        return languageProcessor as KotlinProcessor
    }
}