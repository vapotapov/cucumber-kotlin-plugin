package com.github.vapotapov.plugins.cucumber.kotlin.steps.factory

import com.github.vapotapov.plugins.cucumber.kotlin.steps.KotlinAnnotatedStepDefinition
import com.intellij.psi.PsiElement
import org.jetbrains.plugins.cucumber.steps.AbstractStepDefinition

class KotlinStepDefinitionFactory {

    fun buildStepDefinition(element: PsiElement, annotationClassName: String): AbstractStepDefinition {
        return KotlinAnnotatedStepDefinition(element, annotationClassName)
    }

}