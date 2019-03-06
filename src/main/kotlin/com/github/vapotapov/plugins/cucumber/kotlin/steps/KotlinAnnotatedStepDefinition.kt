package com.github.vapotapov.plugins.cucumber.kotlin.steps

import com.github.vapotapov.plugins.cucumber.kotlin.utils.CucumberKotlinUtil
import com.github.vapotapov.plugins.cucumber.kotlin.utils.CucumberVersionUtil
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.plugins.cucumber.CucumberUtil
import org.jetbrains.plugins.cucumber.steps.AbstractStepDefinition

class KotlinAnnotatedStepDefinition(element: PsiElement, val annotationClassName: String) :
    AbstractStepDefinition(element) {

    override fun getVariableNames(): MutableList<String> {
        val element = element
        val result = mutableListOf<String>()
        if (element is PsiMethod) {
            val parameters = element.parameterList.parameters
            parameters.forEach { parameter ->
                parameter.name?.let {
                    result.add(it)
                }
            }
        }
        return result
    }

    override fun getCucumberRegexFromElement(element: PsiElement): String? {
        val definitionText = stepDefinitionText ?: return null
        val module = ModuleUtilCore.findModuleForPsiElement(element)

        module?.let {
            if (CucumberVersionUtil.isCucumber3OrMore(element)) return definitionText

            if (CucumberKotlinUtil.isCucumberExpression(definitionText)) {
                val parameterTypes = CucumberKotlinUtil.getAllParameterTypes(module)
                return CucumberUtil.buildRegexpFromCucumberExpression(definitionText, parameterTypes)
            }
        }
        return definitionText
    }

    override fun getStepDefinitionText(): String? {
        val element = (element ?: return null) as? PsiMethod ?: return null

        val patternText = CucumberKotlinUtil.getStepAnnotationValue(element, annotationClassName)

        if (patternText != null && patternText.length > 1) return patternText.replace("\\\\", "\\")
            .replace("\\\"", "\"")
        return null
    }
}