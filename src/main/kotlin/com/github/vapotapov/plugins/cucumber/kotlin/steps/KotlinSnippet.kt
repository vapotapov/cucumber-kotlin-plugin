package com.github.vapotapov.plugins.cucumber.kotlin.steps

import com.google.common.primitives.Primitives
import cucumber.runtime.snippets.Snippet

class KotlinSnippet : Snippet {

    override fun tableHint(): String = ""

    override fun escapePattern(pattern: String): String {
        return pattern.replace("\\", "\\\\").replace("\"", "\\\"")
    }

    override fun template(): String = "\n\n@{0}(\"{1}\")\nfun {2}({3})\'{\'\n    // {4}\n{5}    TODO(\"not implemented\")\n\'}\'\n"

    override fun arguments(argumentTypes: MutableList<Class<*>>): String {
        val result = StringBuilder()
        for (i in 0 until argumentTypes.size) {
            val arg = argumentTypes[i]
            if (i > 0) {
                result.append(", ")
            }
            result.append("arg$i: ${getArgType(arg)}")
        }
        return result.toString()
    }

    private fun getArgType(argType: Class<*>): String {
        return if (argType.isPrimitive) {
            checkNotNull(Primitives.wrap(argType).kotlin.simpleName)
        } else argType.simpleName
    }

    override fun namedGroupStart(): String? = null

    override fun namedGroupEnd(): String? = null
}