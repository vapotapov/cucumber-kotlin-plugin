@file:Suppress("InvalidBundleOrProperty")

package com.github.vapotapov.plugins.cucumber.kotlin

import com.intellij.CommonBundle
import org.jetbrains.annotations.PropertyKey
import java.lang.ref.Reference
import java.lang.ref.SoftReference
import java.util.*


object CucumberKotlinBundle {

    const val BUNDLE: String = "com.github.vapotapov.plugins.cucumber.kotlin.CucumberKotlinBundle"

    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
        return CommonBundle.message(getBundle(), key, *params)
    }

    private var ourBundle: Reference<ResourceBundle>? = null

    private fun getBundle(): ResourceBundle {
        var bundle = com.intellij.reference.SoftReference.dereference<ResourceBundle>(ourBundle)
        if (bundle == null) {
            bundle = ResourceBundle.getBundle(BUNDLE)
            ourBundle = SoftReference(bundle)
        }
        return bundle!!
    }
}
//com/github/vapotapov/plugins/cucumber/kotlin/CucumberKotlinBundle