package com.github.vapotapov.plugins.cucumber.kotlin.steps

import com.github.vapotapov.plugins.cucumber.kotlin.utils.CucumberConfigUtil
import org.jetbrains.plugins.cucumber.psi.GherkinFile
import org.jetbrains.plugins.cucumber.psi.GherkinStep
import java.lang.String.format


class AnnotationPackageProvider(private val versionProvider: CucumberVersionProvider = CucumberVersionProvider()) {
    companion object {
        private const val CUCUMBER_1_1_ANNOTATION_BASE_PACKAGE = "cucumber.api.java"
        private const val CUCUMBER_1_0_ANNOTATION_BASE_PACKAGE = "cucumber.annotation"
    }

    fun getAnnotationPackageFor(step: GherkinStep): String {
        return format("%s.%s", annotationBasePackage(step), locale(step))
    }

    private fun locale(step: GherkinStep): String {
        val file = step.containingFile as GherkinFile
        return file.localeLanguage.replace("-".toRegex(), "_")
    }

    private fun annotationBasePackage(step: GherkinStep): String {
        val version = versionProvider.getVersion(step)
        return if (version != null && version < CucumberConfigUtil.CUCUMBER_VERSION_1_1) {
            CUCUMBER_1_0_ANNOTATION_BASE_PACKAGE
        } else CUCUMBER_1_1_ANNOTATION_BASE_PACKAGE
    }
}