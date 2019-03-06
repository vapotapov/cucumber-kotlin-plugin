package com.github.vapotapov.plugins.cucumber.kotlin.utils

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.execution.PsiLocation
import com.intellij.execution.junit2.info.LocationUtil
import com.intellij.find.findUsages.JavaFindUsagesHelper
import com.intellij.find.findUsages.JavaMethodFindUsagesOptions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.ClassUtil
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil.getChildOfType
import com.intellij.psi.util.PsiTreeUtil.getChildrenOfTypeAsList
import com.intellij.usageView.UsageInfo
import com.intellij.util.CommonProcessors
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.plugins.cucumber.CucumberUtil.STANDARD_PARAMETER_TYPES
import org.jetbrains.plugins.cucumber.MapParameterTypeManager
import org.jetbrains.plugins.cucumber.MapParameterTypeManager.DEFAULT
import org.jetbrains.plugins.cucumber.psi.*
import java.util.*
import java.util.regex.Pattern


object CucumberKotlinUtil {
    val STEP_MARKERS: Set<String> = HashSet(Arrays.asList("Given", "Then", "And", "But", "When"))
    val HOOK_MARKERS: Set<String> = HashSet(Arrays.asList("Before", "After"))

    const val CUCUMBER_STEP_ANNOTATION_PREFIX_1_0 = "cucumber.annotation."
    const val CUCUMBER_STEP_ANNOTATION_PREFIX_1_1 = "cucumber.api.java."

    const val PARAMETER_TYPE_CLASS = "io.cucumber.cucumberexpressions.ParameterType"

    const val CUCUMBER_EXPRESSIONS_CLASS_MARKER = "io.cucumber.cucumberexpressions.CucumberExpressionGenerator"

    private val BEGIN_ANCHOR = Pattern.compile("^\\^.*")
    private val END_ANCHOR = Pattern.compile(".*\\$$")
    private val SCRIPT_STYLE_REGEXP = Pattern.compile("^/(.*)/$")
    private val PARENTHESIS = Pattern.compile("\\(([^)]+)\\)")
    private val ALPHA = Pattern.compile("[a-zA-Z]+")

    private val JAVA_PARAMETER_TYPES = mapOf(
        "short" to checkNotNull(STANDARD_PARAMETER_TYPES["int"]),
        "biginteger" to checkNotNull(STANDARD_PARAMETER_TYPES["int"]),
        "bigdecimal" to "-?\\d*[.,]\\d+",
        "byte" to checkNotNull(STANDARD_PARAMETER_TYPES["int"]),
        "double" to checkNotNull(STANDARD_PARAMETER_TYPES["float"]),
        "long" to checkNotNull(STANDARD_PARAMETER_TYPES["int"])
    )

    fun isCucumberExpression(expression: String): Boolean {
        var m = BEGIN_ANCHOR.matcher(expression)
        if (m.find()) {
            return false
        }
        m = END_ANCHOR.matcher(expression)
        if (m.find()) {
            return false
        }
        m = SCRIPT_STYLE_REGEXP.matcher(expression)
        if (m.find()) {
            return false
        }
        m = PARENTHESIS.matcher(expression)
        if (m.find()) {
            val insideParenthesis = m.group(1)
            return ALPHA.matcher(insideParenthesis).lookingAt()
        }
        return true
    }

    fun getAllParameterTypes(module: Module): MapParameterTypeManager {
        val project = module.project
        val manager = PsiManager.getInstance(project)

        val projectDir = project.guessProjectDir()
        val psiDirectory = if (projectDir != null) manager.findDirectory(projectDir) else null
        return if (psiDirectory != null) {
            CachedValuesManager.getCachedValue(
                psiDirectory
            ) {
                CachedValueProvider.Result.create(
                    doGetAllParameterTypes(module),
                    PsiModificationTracker.MODIFICATION_COUNT
                )
            }
        } else DEFAULT
    }

    private fun doGetAllParameterTypes(module: Module): MapParameterTypeManager {
        val dependenciesScope = module.getModuleWithDependenciesAndLibrariesScope(true)
        val processor = CommonProcessors.CollectProcessor<UsageInfo>()
        val options = JavaMethodFindUsagesOptions(dependenciesScope)

        val parameterTypeClass = ClassUtil.findPsiClass(PsiManager.getInstance(module.project), PARAMETER_TYPE_CLASS)

        parameterTypeClass?.let {
            for (constructor in parameterTypeClass.constructors) {
                JavaFindUsagesHelper.processElementUsages(constructor, options, processor)
            }
        }

        val smartPointerManager = SmartPointerManager.getInstance(module.project)
        val values = HashMap<String, String>()
        val declarations = mutableMapOf<String, SmartPsiElementPointer<PsiElement>>()
        for (ui in processor.results) {
            val element = ui.element
            if (element != null && element.parent is PsiNewExpression) {
                val newExpression = element.parent as PsiNewExpression
                val arguments = newExpression.argumentList
                if (arguments != null) {
                    val expressions = arguments.expressions
                    if (expressions.size > 1) {
                        val evaluationHelper = JavaPsiFacade.getInstance(module.project).constantEvaluationHelper

                        var constantValue =
                            evaluationHelper.computeConstantExpression(expressions[0], false) ?: continue
                        val name = constantValue.toString()

                        constantValue = evaluationHelper.computeConstantExpression(expressions[1], false)
                        if (constantValue == null) {
                            continue
                        }
                        val value = constantValue.toString()
                        values[name] = value

                        val smartPointer = smartPointerManager.createSmartPsiElementPointer(expressions[0])
                        declarations[name] = smartPointer as SmartPsiElementPointer<PsiElement>
                    }
                }
            }
        }
        values.putAll(STANDARD_PARAMETER_TYPES)
        values.putAll(JAVA_PARAMETER_TYPES)
        return MapParameterTypeManager(values, declarations)
    }

    fun getStepAnnotationValue(method: PsiMethod, annotationClassName: String?): String? {
        val stepAnnotation = getCucumberStepAnnotation(method, annotationClassName) ?: return null

        return getAnnotationValue(stepAnnotation)
    }

    fun getCucumberStepAnnotation(method: PsiMethod): PsiAnnotation? {
        return ReadAction.compute<PsiAnnotation?, Throwable> { getCucumberStepAnnotation(method, null) }
    }

    fun isStepDefinition(method: PsiMethod): Boolean {
        val stepAnnotation = getCucumberStepAnnotation(method)
        return stepAnnotation != null && getAnnotationValue(stepAnnotation) != null
    }

    private fun getCucumberStepAnnotation(method: PsiMethod, annotationClassName: String?): PsiAnnotation? {
        if (!method.hasModifierProperty(PsiModifier.PUBLIC)) {
            return null
        }

        val annotations = method.modifierList.annotations

        for (annotation in annotations) {
            if (annotation != null &&
                (annotationClassName == null || annotationClassName == annotation.qualifiedName) &&
                isCucumberStepAnnotation(annotation)
            ) {
                return annotation
            }
        }
        return null
    }

    private fun getPackageOfStepDef(steps: Array<GherkinStep>): String? {
        for (step in steps) {
            val pack = getPackageOfStep(step)
            if (pack != null) return pack
        }
        return null
    }

    fun getPackageOfStepDef(element: PsiElement): String {
        val file = element.containingFile
        if (file is GherkinFile) {
            val feature = getChildOfType(file, GherkinFeature::class.java)
            if (feature != null) {
                val scenarioList = getChildrenOfTypeAsList(feature, GherkinScenario::class.java)
                for (scenario in scenarioList) {
                    val result = getPackageOfStepDef(scenario.steps)
                    if (result != null) {
                        return result
                    }
                }

                val scenarioOutlineList = getChildrenOfTypeAsList(feature, GherkinScenarioOutline::class.java)
                for (scenario in scenarioOutlineList) {
                    val result = getPackageOfStepDef(scenario.steps)
                    if (result != null) {
                        return result
                    }
                }
            }
        }
        return ""
    }


    private fun getAnnotationName(annotation: PsiAnnotation): String? {
        val qualifiedAnnotationName = Ref<String>()
        ApplicationManager.getApplication().runReadAction {
            val qualifiedName = annotation.qualifiedName
            qualifiedAnnotationName.set(qualifiedName)
        }
        return qualifiedAnnotationName.get()
    }

    fun getAnnotationValue(stepAnnotation: PsiAnnotation): String? {
        return AnnotationUtil.getDeclaredStringAttributeValue(stepAnnotation, "value")
    }

    fun isCucumberStepAnnotation(annotation: PsiAnnotation): Boolean {
        val annotationName = getAnnotationName(annotation) ?: return false

        val annotationSuffix = getCucumberAnnotationSuffix(annotationName)
        return if (annotationSuffix.contains(".")) {
            true
        } else STEP_MARKERS.contains(annotationName)
    }

    fun getPackageOfStep(step: GherkinStep): String? {
        for (ref in step.references) {
            val refElement = ref.resolve()
            if (refElement is PsiMethod || refElement is PsiMethodCallExpression) {
                val file = refElement.containingFile as PsiClassOwner
                val packageName = file.packageName
                if (StringUtil.isNotEmpty(packageName)) {
                    return packageName
                }
            }
        }
        return null
    }

    @JvmStatic
    fun addGlue(glue: String, glues: MutableSet<String>) {
        var covered = false
        val toRemove = ContainerUtil.newHashSet<String>()

        glues.forEach {
            if (glue.startsWith("$it.")) {
                covered = true
                return@forEach
            } else if (it.startsWith("$glue.")) {
                toRemove.add(it)
            }
        }
        toRemove.forEach { glues.remove(it) }

        if (!covered) glues.add(glue)
    }

    fun isCucumberExpressionsAvailable(context: PsiElement): Boolean {
        val location = PsiLocation(context)
        return LocationUtil.isJarAttached(location, PsiDirectory.EMPTY_ARRAY, CUCUMBER_EXPRESSIONS_CLASS_MARKER)
    }

    fun getPatternFromStepDefinition(stepAnnotation: PsiAnnotation): String? {
        return ReadAction.compute<String?, Throwable> {
            var result = AnnotationUtil.getStringAttributeValue(stepAnnotation, null)
            result?.let {
                result = it.replace("\\\\".toRegex(), "\\\\\\\\")
            }
            result
        }
    }

    fun getCucumberPendingExceptionFqn(context: PsiElement): String {
        val version = CucumberConfigUtil.getCucumberCoreVersion(context)
        return if (version == null || version.compareTo(CucumberConfigUtil.CUCUMBER_VERSION_1_1) >= 0) {
            "cucumber.api.PendingException"
        } else "cucumber.runtime.PendingException"
    }

    private fun getCucumberAnnotationSuffix(name: String): String {
        return when {
            name.startsWith(CUCUMBER_STEP_ANNOTATION_PREFIX_1_0) -> name.substring(CUCUMBER_STEP_ANNOTATION_PREFIX_1_0.length)
            name.startsWith(CUCUMBER_STEP_ANNOTATION_PREFIX_1_1) -> name.substring(CUCUMBER_STEP_ANNOTATION_PREFIX_1_1.length)
            else -> ""
        }
    }
}