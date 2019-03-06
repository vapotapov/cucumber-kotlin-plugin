package com.github.vapotapov.plugins.cucumber.kotlin.steps.run

import com.intellij.execution.application.JvmMainMethodRunConfigurationOptions
import com.intellij.util.xmlb.annotations.OptionTag

open class CucumberJavaConfigurationOptions: JvmMainMethodRunConfigurationOptions() {
    @get:OptionTag("GLUE")
    open var glue: String? by string()

    @get:OptionTag("FILE_PATH")
    open var filePath: String? by string()

    @get:OptionTag("NAME_FILTER")
    open var nameFilter: String? by string()

    @get:OptionTag("SUGGESTED_NAME")
    open var suggestedName: String? by string()

    @get:OptionTag("CUCUMBER_CORE_VERSION")
    open var cucumberCoreVersion: String? by string()
}