package com.cquilez.pitesthelper.services

import com.cquilez.pitesthelper.application.port.BuildSystemProcessor
import com.cquilez.pitesthelper.application.port.out.ExtensionsOutPort
import com.cquilez.pitesthelper.exception.PitestHelperException
import com.cquilez.pitesthelper.infrastructure.extensions.GradleProcessor
import com.cquilez.pitesthelper.infrastructure.extensions.KotlinProcessor
import com.cquilez.pitesthelper.application.port.LanguageProcessor
import com.cquilez.pitesthelper.infrastructure.dto.ProjectAnalysisRequestDTO
import com.cquilez.pitesthelper.infrastructure.extensions.MavenProcessor
import com.intellij.openapi.components.Service
import com.intellij.openapi.extensions.ExtensionPointName

@Service
class ExtensionsService : ExtensionsOutPort {

    companion object {
        private val LANGUAGE_PROCESSORS =
            ExtensionPointName.create<LanguageProcessor>("com.cquilez.pitesthelper.languageProcessor")

        private val BUILD_SYSTEM_PROCESSORS =
            ExtensionPointName.create<BuildSystemProcessor<ProjectAnalysisRequestDTO>>("com.cquilez.pitesthelper.buildSystemProcessor")
    }

    override fun <T> getMavenExtension(): BuildSystemProcessor<T> {
        val mavenExtension = BUILD_SYSTEM_PROCESSORS.extensionList.firstOrNull { it is MavenProcessor }
        if (mavenExtension == null) {
            throw PitestHelperException("You need to enable Maven plugin to run analysis on Maven projects.")
        }
        return mavenExtension as BuildSystemProcessor<T>
    }

    override fun <T> getGradleExtension(): BuildSystemProcessor<T> {
        val gradleExtension = BUILD_SYSTEM_PROCESSORS.extensionList.firstOrNull { it is GradleProcessor }
        if (gradleExtension == null) {
            throw PitestHelperException("You need to enable Gradle plugin to run analysis on Gradle projects.")
        }
        return gradleExtension as BuildSystemProcessor<T>
    }

    override fun getKotlinExtension(): KotlinProcessor {
        val languageProcessor = LANGUAGE_PROCESSORS.extensionList.firstOrNull { it is KotlinProcessor }
        if (languageProcessor == null) {
            throw PitestHelperException("You need to enable Kotlin plugin to work with Kotlin classes.")
        }
        return languageProcessor as KotlinProcessor
    }
}