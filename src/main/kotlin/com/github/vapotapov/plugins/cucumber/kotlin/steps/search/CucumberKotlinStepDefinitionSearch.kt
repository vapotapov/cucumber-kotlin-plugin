package com.github.vapotapov.plugins.cucumber.kotlin.steps.search

import com.github.vapotapov.plugins.cucumber.kotlin.utils.CucumberKotlinUtil
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import com.intellij.util.QueryExecutor
import com.intellij.openapi.application.ReadAction
import org.jetbrains.plugins.cucumber.CucumberUtil
import com.intellij.psi.PsiAnnotation

class CucumberKotlinStepDefinitionSearch: QueryExecutor<PsiReference, ReferencesSearch.SearchParameters> {

    override fun execute(
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>
    ): Boolean {
        val method = queryParameters.elementToSearch as? PsiMethod ?: return true

        val isStepDefinition: Boolean = ReadAction.compute<Boolean, Throwable> { CucumberKotlinUtil.isStepDefinition(method) }
        if ((!isStepDefinition)) {
            return true
        }

        val stepAnnotation = ReadAction.compute<PsiAnnotation, Throwable> { CucumberKotlinUtil.getCucumberStepAnnotation(method) }

        val regexp = CucumberKotlinUtil.getPatternFromStepDefinition(stepAnnotation) ?: return true
        return CucumberUtil.findGherkinReferencesToElement(
            method,
            regexp,
            consumer,
            queryParameters.effectiveSearchScope
        )
    }
}