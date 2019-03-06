package com.github.vapotapov.plugins.cucumber.kotlin.steps.run;


import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.components.BaseState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import icons.CucumberJavaIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class CucumberJavaRunConfigurationType extends ConfigurationTypeBase {
    public CucumberJavaRunConfigurationType() {
        super("CucumberJavaRunConfigurationType", "Cucumber java", null,
                NotNullLazyValue.createValue(() -> CucumberJavaIcons.CucumberJavaRunConfiguration));
        addFactory(new ConfigurationFactory(this) {
            @Override
            public Icon getIcon() {
                return CucumberJavaRunConfigurationType.this.getIcon();
            }

            @Override
            @NotNull
            public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
                return new CucumberJavaRunConfiguration(getDisplayName(), project, this);
            }

            @Override
            public Class<? extends BaseState> getOptionsClass() {
                return CucumberJavaConfigurationOptions.class;
            }
        });
    }

    @Override
    public String getHelpTopic() {
        return "reference.dialogs.rundebug.CucumberJavaRunConfigurationType";
    }

    @NotNull
    public static CucumberJavaRunConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(CucumberJavaRunConfigurationType.class);
    }
}