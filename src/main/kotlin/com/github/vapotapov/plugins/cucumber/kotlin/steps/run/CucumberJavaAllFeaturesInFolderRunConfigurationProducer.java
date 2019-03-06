package com.github.vapotapov.plugins.cucumber.kotlin.steps.run;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.openapi.util.NullableComputable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.cucumber.CucumberBundle;
import org.jetbrains.plugins.cucumber.CucumberJvmExtensionPoint;
import org.jetbrains.plugins.cucumber.psi.GherkinFile;

import java.util.List;
import java.util.Set;

public class CucumberJavaAllFeaturesInFolderRunConfigurationProducer extends CucumberJavaRunConfigurationProducer {

    public CucumberJavaAllFeaturesInFolderRunConfigurationProducer(ConfigurationFactory configurationFactory) {
        super(configurationFactory);
    }

    public CucumberJavaAllFeaturesInFolderRunConfigurationProducer(ConfigurationType configurationType) {
        super(configurationType);
    }

    @Override
    protected NullableComputable<String> getStepsGlue(@NotNull final PsiElement element) {
        final Set<String> glues = getHookGlue(element);
        if (element instanceof PsiDirectory) {
            final PsiDirectory dir = (PsiDirectory) element;
            final List<CucumberJvmExtensionPoint> extensions = CucumberJvmExtensionPoint.EP_NAME.getExtensionList();
            return new NullableComputable<String>() {
                @NotNull
                @Override
                public String compute() {
                    dir.accept(new PsiElementVisitor() {
                        @Override
                        public void visitFile(final PsiFile file) {
                            if (file instanceof GherkinFile) {
                                for (CucumberJvmExtensionPoint extension : extensions) {
                                    extension.getGlues((GherkinFile) file, glues);
                                }
                            }
                        }

                        @Override
                        public void visitDirectory(PsiDirectory dir) {
                            for (PsiDirectory subDir : dir.getSubdirectories()) {
                                subDir.accept(this);
                            }

                            for (PsiFile file : dir.getFiles()) {
                                file.accept(this);
                            }
                        }
                    });

                    return StringUtil.join(glues, " ");
                }
            };
        }
        return null;
    }

    @Override
    protected String getConfigurationName(@NotNull final ConfigurationContext context) {
        final PsiElement element = context.getPsiLocation();
        return CucumberBundle.message("cucumber.run.all.features", ((PsiDirectory) element).getVirtualFile().getName());
    }

    @Nullable
    @Override
    protected VirtualFile getFileToRun(ConfigurationContext context) {
        final PsiElement element = context.getPsiLocation();
        if (element instanceof PsiDirectory) {
            return ((PsiDirectory) element).getVirtualFile();
        }
        return null;
    }
}