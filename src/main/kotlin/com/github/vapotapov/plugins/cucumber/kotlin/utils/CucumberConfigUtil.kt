package com.github.vapotapov.plugins.cucumber.kotlin.utils

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import java.util.regex.Pattern


object CucumberConfigUtil {
    private val CUCUMBER_PATTERN = Pattern.compile("cucumber-core-(.*)\\.jar")

    private val CUCUMBER_CLI_MAIN_1_0 = "cucumber.cli.Main"

    private val CUCUMBER_API_CLI_MAIN_1_1 = "cucumber.api.cli.Main"

    val CUCUMBER_VERSION_1_0 = "1.0"

    val CUCUMBER_VERSION_1_1 = "1.1"

    fun getCucumberCoreVersion(place: PsiElement): String? {
        val module = ModuleUtilCore.findModuleForPsiElement(place) ?: return null

        return CachedValuesManager.getManager(module.project).getCachedValue(
            module
        ) {
            CachedValueProvider.Result
                .create(getCucumberCoreVersionImpl(module), ProjectRootManager.getInstance(module.project))
        }
    }

    private fun getCucumberCoreVersionImpl(module: Module?): String? {
        for (orderEntry in ModuleRootManager.getInstance(module!!).orderEntries) {
            if (orderEntry is LibraryOrderEntry) {
                val libraryName = orderEntry.libraryName
                val library = orderEntry.library

                //libraryName is null for simple jar entries
                if ((libraryName == null || libraryName.toLowerCase().contains("cucumber")) && library != null) {
                    val files = library.getFiles(OrderRootType.CLASSES)
                    for (file in files) {
                        val version = getVersionByFile(file)
                        if (version != null) return version
                    }
                }
            }
        }
        return getSimpleVersionFromMainClass(module)
    }

    private fun getSimpleVersionFromMainClass(module: Module): String? {
        val facade = JavaPsiFacade.getInstance(module.getProject())

        val oldMain = facade.findClass(CUCUMBER_CLI_MAIN_1_0, module.moduleWithLibrariesScope)
        oldMain?.let {return CUCUMBER_VERSION_1_0}

        val newMain = facade.findClass(CUCUMBER_API_CLI_MAIN_1_1, module.moduleWithLibrariesScope)
        return if (newMain != null) CUCUMBER_VERSION_1_1 else null

    }

    private fun getVersionByFile(file: VirtualFile): String? {
        val name = file.name
        val matcher = CUCUMBER_PATTERN.matcher(name)
        return if (matcher.matches() && matcher.groupCount() == 1) {
            matcher.group(1)
        } else null
    }
}