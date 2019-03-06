package com.github.vapotapov.plugins.cucumber.kotlin

import com.github.vapotapov.plugins.cucumber.kotlin.steps.KotlinStepDefinitionCreator
import com.github.vapotapov.plugins.cucumber.kotlin.steps.factory.KotlinStepDefinitionFactory
import com.github.vapotapov.plugins.cucumber.kotlin.utils.CucumberKotlinUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.plugins.cucumber.BDDFrameworkType
import org.jetbrains.plugins.cucumber.StepDefinitionCreator
import org.jetbrains.plugins.cucumber.psi.GherkinFile
import org.jetbrains.plugins.cucumber.psi.GherkinRecursiveElementVisitor
import org.jetbrains.plugins.cucumber.psi.GherkinStep
import org.jetbrains.plugins.cucumber.steps.AbstractCucumberExtension
import org.jetbrains.plugins.cucumber.steps.AbstractStepDefinition

class CucumberKotlinExtension : AbstractCucumberExtension() {

    val CUCUMBER_RUNTIME_JAVA_STEP_DEF_ANNOTATION = "cucumber.runtime.java.StepDefAnnotation"

    override fun getStepDefinitionCreator(): StepDefinitionCreator = KotlinStepDefinitionCreator()

    override fun isWritableStepLikeFile(child: PsiElement, parent: PsiElement): Boolean {
        if (child is PsiClassOwner) {
            val file = child.getContainingFile()
            file?.let {
                val vFile = file.virtualFile
                vFile?.let {
                    val rootForFile =
                        ProjectRootManager.getInstance(child.getProject()).fileIndex.getSourceRootForFile(vFile)
                    return rootForFile != null
                }
            }
        }
        return false
    }

    override fun getStepDefinitionContainers(featureFile: GherkinFile): MutableCollection<out PsiFile> {
        val result = mutableSetOf<PsiFile>()

        val module = ModuleUtilCore.findModuleForPsiElement(featureFile) ?: return mutableSetOf()
        val stepDefs = loadStepsFor(featureFile, module)

        stepDefs.forEach {
            val stepDefElement = it.element

            stepDefElement?.let { element ->
                val psiFile = element.containingFile
                val psiDirectory = psiFile.parent
                psiDirectory?.let {
                    if (isWritableStepLikeFile(psiFile, psiDirectory)) result.add(psiFile)
                }
            }
        }
        return result
    }

    override fun getStepFileType() = BDDFrameworkType(KotlinFileType.INSTANCE)

    override fun isStepLikeFile(child: PsiElement, parent: PsiElement): Boolean {
        if (child is PsiClassOwner) return true
        return false
    }

    override fun getGlues(file: GherkinFile, gluesFromOtherFiles: MutableSet<String>?): MutableCollection<String> {
        var glues = gluesFromOtherFiles ?: ContainerUtil.newHashSet()

        file.accept(object: GherkinRecursiveElementVisitor(){
            override fun visitStep(step: GherkinStep?) {
                step?.let {
                    val glue = CucumberKotlinUtil.getPackageOfStep(step)
                    glue?.let {
                        CucumberKotlinUtil.addGlue(glue, glues)
                    }
                }
            }
        } )
        return glues
    }

    override fun loadStepsFor(featureFile: PsiFile?, module: Module): MutableList<AbstractStepDefinition> {
        val dependenciesScope = module.getModuleWithDependenciesAndLibrariesScope(true)

        var stepDefAnnotationClass = JavaPsiFacade.getInstance(module.project).findClass(
            CUCUMBER_RUNTIME_JAVA_STEP_DEF_ANNOTATION,
            dependenciesScope
        )
            ?: return mutableListOf()

        val stepDefFactory = KotlinStepDefinitionFactory()
        val result = mutableListOf<AbstractStepDefinition>()

        val stepDefAnnotations = AnnotatedElementsSearch.searchPsiClasses(stepDefAnnotationClass, dependenciesScope)

        stepDefAnnotations.forEach {
            val annotationClassName = it.qualifiedName
            if (it.isAnnotationType && annotationClassName != null) {
                val stepDefinitions = AnnotatedElementsSearch.searchPsiMethods(it, dependenciesScope)
                stepDefinitions.forEach { method ->
                    run {
                        val stepDefinition = stepDefFactory.buildStepDefinition(method, annotationClassName)
                        result.add(stepDefinition)
                    }
                }
            }
        }
        return result
    }
}