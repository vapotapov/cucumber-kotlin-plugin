package com.github.vapotapov.plugins.cucumber.kotlin.steps.search

import com.github.vapotapov.plugins.cucumber.kotlin.utils.CucumberKotlinUtil
import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import org.jetbrains.plugins.cucumber.CucumberUtil
import org.jetbrains.plugins.cucumber.psi.GherkinFileType

class CucumberKotlinMethodUsageSearcher : QueryExecutorBase<PsiReference, MethodReferencesSearch.SearchParameters>() {
    override fun processQuery(
        parameters: MethodReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>
    ) {
        val scope = parameters.effectiveSearchScope as? GlobalSearchScope ?: return

        val method = parameters.method

        val stepAnnotation = CucumberKotlinUtil.getCucumberStepAnnotation(method)

        val regexp =
            if (stepAnnotation != null) CucumberKotlinUtil.getPatternFromStepDefinition(stepAnnotation) else return

        val word = CucumberUtil.getTheBiggestWordToSearchByIndex(regexp!!)
        if (StringUtil.isEmpty(word)) return
        val restrictedScope = GlobalSearchScope.getScopeRestrictedByFileTypes(scope, GherkinFileType.INSTANCE)

        ReadAction.run<Throwable> {
            ReferencesSearch.search(
                ReferencesSearch.SearchParameters(
                    method,
                    restrictedScope,
                    false,
                    parameters.optimizer
                )
            ).forEach(consumer)
        }
    }
}
