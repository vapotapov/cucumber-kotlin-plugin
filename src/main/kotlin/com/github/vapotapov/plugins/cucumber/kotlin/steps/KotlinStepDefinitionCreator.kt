package com.github.vapotapov.plugins.cucumber.kotlin.steps

import com.github.vapotapov.plugins.cucumber.kotlin.utils.CucumberKotlinUtil
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.file.PsiDirectoryFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ObjectUtils
import cucumber.runtime.snippets.CamelCaseConcatenator
import cucumber.runtime.snippets.FunctionNameGenerator
import cucumber.runtime.snippets.SnippetGenerator
import gherkin.formatter.model.Step
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.idea.decompiler.classFile.KtClsFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.getOrCreateBody
import org.jetbrains.plugins.cucumber.AbstractStepDefinitionCreator
import org.jetbrains.plugins.cucumber.psi.GherkinStep
import java.io.IOException
import java.nio.charset.Charset
import java.util.*


class KotlinStepDefinitionCreator : AbstractStepDefinitionCreator() {

    override fun createStepDefinitionContainer(dir: PsiDirectory, name: String): PsiFile {
        val templateText = javaClass.getResource("/fileTemplates/kotlin_file.ft").readText(Charset.forName("UTF-8"))
        val template = FileTemplateManager.getInstance(dir.project).addTemplate("Default", "kt")
            .apply { text = templateText }

        return FileTemplateUtil.createFromTemplate(template, name, null, dir).containingFile
    }

    override fun validateNewStepDefinitionFileName(project: Project, name: String): Boolean {
        if (name.isEmpty()) return false
        if (!Character.isJavaIdentifierStart(name.codePointAt(0))) return false
        for (i in 1 until name.length) {
            if (!Character.isJavaIdentifierPart(name.codePointAt(i))) return false
        }
        return true
    }

    override fun createStepDefinition(step: GherkinStep, file: PsiFile): Boolean {
        if (file !is PsiClassOwner) return false

        val project = file.project
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        checkNotNull(editor)
        closeActiveTemplateBuilders(file)
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        val clazz = PsiTreeUtil.getChildOfType(file, KtLightClass::class.java)

        clazz?.let {
            val stepDefText = buildStepDefinitionByStep(step)
            val stepDefinition = KtPsiFactory(clazz, false).createFunction(stepDefText)

            val body = clazz.kotlinOrigin?.getOrCreateBody()

            val children = body?.children
            children?.let {
                val anchor = it[it.size - 1]

                body.addAfter(stepDefinition, anchor)
            }
        }
        return true
    }

    override fun getStepDefinitionFilePath(file: PsiFile): String {
        val vFile = file.virtualFile
        var result = "Not applicable for Kotlin - ${file.name}"

        if (file is PsiClassOwner && vFile != null && !vFile.name.endsWith(".java")) {
            val packageName = file.packageName
            result = if (StringUtil.isEmptyOrSpaces(packageName)) {
                "Kotlin - ${vFile.nameWithoutExtension}"
            } else {
                "Kotlin - ${vFile.nameWithoutExtension + " (" + packageName + ")"}"
            }
        }
        return result
    }

    override fun getDefaultStepFileName(p0: GherkinStep): String = STEP_DEFINITION_SUFFIX

    override fun getDefaultStepDefinitionFolder(step: GherkinStep): PsiDirectory {
        val featureFile = step.containingFile
        if (featureFile != null) {
            val psiDirectory = featureFile.containingDirectory
            val project = step.project
            if (psiDirectory != null) {
                val projectFileIndex = ProjectRootManager.getInstance(project).fileIndex
                val directory = psiDirectory.virtualFile
                if (projectFileIndex.isInContent(directory)) {
                    var sourceRoot = projectFileIndex.getSourceRootForFile(directory)
                    val module = projectFileIndex.getModuleForFile(featureFile.virtualFile)
                    if (module != null) {
                        val sourceRoots = ModuleRootManager.getInstance(module).sourceRoots
                        if (sourceRoot != null && sourceRoot.name == "resources") {
                            val resourceParent = sourceRoot.parent
                            for (vFile in sourceRoots) {
                                if (vFile.path.startsWith(resourceParent.path) && vFile.name == "java") {
                                    sourceRoot = vFile
                                    break
                                }
                            }
                        } else {
                            if (sourceRoots.isNotEmpty()) {
                                sourceRoot = sourceRoots[sourceRoots.size - 1]
                            }
                        }
                    }
                    var packageName = ""
                    if (sourceRoot != null) {
                        packageName = CucumberKotlinUtil.getPackageOfStepDef(step)
                    }

                    val packagePath = packageName.replace('.', '/')
                    val path = sourceRoot?.path ?: directory.path
                    // ToDo: I shouldn't create directories, only create VirtualFile object.
                    val resultRef = Ref<PsiDirectory>()
                    try {
                        WriteAction.runAndWait<IOException> {
                            val packageFile = VfsUtil.createDirectoryIfMissing("$path/$packagePath")
                            if (packageFile != null) {
                                resultRef.set(PsiDirectoryFactory.getInstance(project).createDirectory(packageFile))
                            }
                        }
                    } catch (ignored: IOException) {

                    }
                    return resultRef.get()
                }
            }
        }
        assert(featureFile != null)
        return ObjectUtils.assertNotNull(featureFile!!.parent)
    }

    companion object {
        private val STEP_DEFINITION_SUFFIX = "MySteps"

        fun buildStepDefinitionByStep(step: GherkinStep): String {
            val cucumberStep = Step(ArrayList(), step.keyword.text, step.stepName, 0, null, null)
            val generator = SnippetGenerator(KotlinSnippet())

            return generator.getSnippet(cucumberStep, FunctionNameGenerator(CamelCaseConcatenator()))
        }
    }
}