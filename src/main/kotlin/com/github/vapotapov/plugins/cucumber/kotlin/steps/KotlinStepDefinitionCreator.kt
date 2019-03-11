package com.github.vapotapov.plugins.cucumber.kotlin.steps

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import cucumber.runtime.snippets.CamelCaseConcatenator
import cucumber.runtime.snippets.FunctionNameGenerator
import cucumber.runtime.snippets.SnippetGenerator
import gherkin.formatter.model.Step
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.getOrCreateBody
import org.jetbrains.plugins.cucumber.java.steps.JavaStepDefinitionCreator
import org.jetbrains.plugins.cucumber.psi.GherkinStep
import java.nio.charset.Charset
import java.util.*


class KotlinStepDefinitionCreator : JavaStepDefinitionCreator() {

    override fun createStepDefinitionContainer(dir: PsiDirectory, name: String): PsiFile {
        val templateText = javaClass.getResource("/fileTemplates/kotlin_file.ft").readText(Charset.forName("UTF-8"))
        val template = FileTemplateManager.getInstance(dir.project).addTemplate("Default", "kt")
            .apply { text = templateText }

        return FileTemplateUtil.createFromTemplate(template, name, null, dir).containingFile
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
        var result = "N/A for Kotlin - ${file.name}"

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

    companion object {
        fun buildStepDefinitionByStep(step: GherkinStep): String {
            val cucumberStep = Step(ArrayList(), step.keyword.text, step.stepName, 0, null, null)
            val generator = SnippetGenerator(KotlinSnippet())

            return generator.getSnippet(cucumberStep, FunctionNameGenerator(CamelCaseConcatenator()))
        }
    }
}