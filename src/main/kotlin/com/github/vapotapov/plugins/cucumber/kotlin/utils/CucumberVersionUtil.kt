package com.github.vapotapov.plugins.cucumber.kotlin.utils

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.text.VersionComparatorUtil

object CucumberVersionUtil {


    val CUCUMBER_CORE_VERSION_4 = "4"
    @JvmField
    val CUCUMBER_CORE_VERSION_3 = "3"
    @JvmField
    val CUCUMBER_CORE_VERSION_2 = "2"
    @JvmField
    val CUCUMBER_CORE_VERSION_1_2 = "1.2"
    @JvmField
    val CUCUMBER_CORE_VERSION_1_0 = "1"

    @JvmField
    val CUCUMBER_1_2_PLUGIN_CLASS = "cucumber.api.Plugin"
    @JvmField
    val CUCUMBER_2_CLASS_MARKER = "cucumber.api.formatter.Formatter"
    @JvmField
    val CUCUMBER_3_CLASS_MARKER = "cucumber.runner.TestCase"
    @JvmField
    val CUCUMBER_4_CLASS_MARKER = "cucumber.api.event.ConcurrentEventListener"

    @JvmStatic
    fun getCucumberCoreVersion(module: Module?, project: Project): String {
        val manager = CachedValuesManager.getManager(project)

        val result = manager.createCachedValue({
            val resultVersion = computeCucumberVersion(module, project)
            CachedValueProvider.Result.create(resultVersion, PsiModificationTracker.MODIFICATION_COUNT)
        }, false)

        return result.value
    }

    fun isCucumber2OrMore(module: Module) =
        VersionComparatorUtil.compare(getCucumberCoreVersion(module, module.project), CUCUMBER_CORE_VERSION_2) >= 0

    fun isCucumber3OrMore(context: PsiElement): Boolean {
        val module = ModuleUtilCore.findModuleForPsiElement(context)
        return VersionComparatorUtil.compare(
            getCucumberCoreVersion(module, context.project),
            CUCUMBER_CORE_VERSION_3
        ) >= 0
    }

    private fun computeCucumberVersion(module: Module?, project: Project): String {
        val scope = if (module != null) GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(
            module,
            true
        ) else GlobalSearchScope.projectScope(project)

        val facade = JavaPsiFacade.getInstance(project)

        facade.findClass(CUCUMBER_4_CLASS_MARKER, scope)?.let {
            return CUCUMBER_CORE_VERSION_4
        }

        facade.findClass(CUCUMBER_3_CLASS_MARKER, scope)?.let {
            return CUCUMBER_CORE_VERSION_3
        }

        facade.findClass(CUCUMBER_2_CLASS_MARKER, scope)?.let {
            return CUCUMBER_CORE_VERSION_2
        }

        facade.findClass(CUCUMBER_1_2_PLUGIN_CLASS, scope)?.let {
            return CUCUMBER_CORE_VERSION_1_2
        }

        return CUCUMBER_CORE_VERSION_3
    }

}