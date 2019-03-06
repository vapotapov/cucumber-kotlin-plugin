package com.github.vapotapov.plugins.cucumber.kotlin.steps

import com.github.vapotapov.plugins.cucumber.kotlin.utils.CucumberConfigUtil
import org.jetbrains.plugins.cucumber.psi.GherkinStep


class CucumberVersionProvider {

    fun getVersion(step: GherkinStep): String? = CucumberConfigUtil.getCucumberCoreVersion(step)

}