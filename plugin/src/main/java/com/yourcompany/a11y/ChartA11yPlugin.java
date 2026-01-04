package com.yourcompany.a11y;

import com.android.build.api.variant.AndroidComponentsExtension;
import com.android.build.api.variant.Variant;
import com.yourcompany.a11y.tasks.GenerateDescriptionsTask;
import com.yourcompany.a11y.tasks.ProcessLayoutsTask;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.util.Collections;

/**
 * Chart Accessibility Gradle Plugin entry point.
 *
 * This plugin provides build-time generation of accessibility descriptions for chart components.
 * It registers two tasks per variant:
 * - generateChartA11y{Variant}: Generates string resources with accessibility descriptions
 * - processChartA11yLayouts{Variant}: Processes layout XML files to inject a11y attributes
 */
public class ChartA11yPlugin implements Plugin<Project> {

    private static final String EXTENSION_NAME = "chartA11y";

    @Override
    public void apply(Project project) {
        // Create extension
        ChartA11yExtension extension = project.getExtensions()
                .create(EXTENSION_NAME, ChartA11yExtension.class, project);

        // Apply plugin after Android plugin is applied
        project.getPlugins().withId("com.android.application", plugin -> {
            configureAndroidProject(project, extension);
        });

        project.getPlugins().withId("com.android.library", plugin -> {
            configureAndroidProject(project, extension);
        });
    }

    @SuppressWarnings("unchecked")
    private void configureAndroidProject(Project project, ChartA11yExtension extension) {
        AndroidComponentsExtension<?, ?, ?> androidComponents = project.getExtensions()
                .getByType(AndroidComponentsExtension.class);

        androidComponents.onVariants(androidComponents.selector().all(), variant -> {
            registerTasks(project, extension, (Variant) variant);
        });
    }

    private void registerTasks(Project project, ChartA11yExtension extension, Variant variant) {
        String variantName = capitalize(variant.getName());

        // Register GenerateDescriptionsTask
        TaskProvider<GenerateDescriptionsTask> generateTask = project.getTasks()
                .register("generateChartA11y" + variantName, GenerateDescriptionsTask.class, task -> {
                    task.setGroup("chart-a11y");
                    task.setDescription("Generate accessibility descriptions for " + variant.getName());

                    // Configure task inputs
                    File configFile = extension.resolveConfigFile();
                    if (configFile != null) {
                        task.getConfigFile().set(configFile);
                    }

                    task.getApiEndpoint().set(extension.getApiEndpoint());
                    task.getEnableCache().set(extension.getEnableCache());
                    task.getCacheDir().set(extension.getCacheDir());
                    task.getApiTimeout().set(extension.getTimeout());
                    task.getConcurrency().set(extension.getConcurrency());
                    task.getFailOnError().set(extension.getFailOnError());

                    // Output directory
                    File outputDir = new File(project.getBuildDir(),
                            "generated/res/a11y/" + variant.getName());
                    task.getOutputDir().set(outputDir);
                });

        // Register generated resources
        Provider<Directory> generatedResDir = generateTask.flatMap(task -> task.getOutputDir());
        variant.getSources().getRes().addGeneratedSourceDirectory(
                generateTask,
                GenerateDescriptionsTask::getOutputDir
        );

        // Register ProcessLayoutsTask
        TaskProvider<ProcessLayoutsTask> processLayoutsTask = project.getTasks()
                .register("processChartA11yLayouts" + variantName, ProcessLayoutsTask.class, task -> {
                    task.setGroup("chart-a11y");
                    task.setDescription("Process layout files for accessibility for " + variant.getName());

                    // Input: src/main/res/layout
                    File layoutDir = new File(project.getProjectDir(), "src/main/res/layout");
                    task.getLayoutDir().set(layoutDir);

                    // Output directory
                    File outputDir = new File(project.getBuildDir(),
                            "generated/res/a11y-layouts/" + variant.getName());
                    task.getOutputDir().set(outputDir);
                });

        variant.getSources().getRes().addGeneratedSourceDirectory(
                processLayoutsTask,
                ProcessLayoutsTask::getOutputDir
        );

        // Make generate{Variant}Resources depend on our tasks
        project.afterEvaluate(p -> {
            String generateResourcesTaskName = "generate" + variantName + "Resources";
            Task generateResourcesTask = p.getTasks().findByName(generateResourcesTaskName);
            if (generateResourcesTask != null) {
                generateResourcesTask.dependsOn(generateTask);
                generateResourcesTask.dependsOn(processLayoutsTask);
            }

            // Also depend on pre-build tasks
            String preBuildTaskName = "pre" + variantName + "Build";
            Task preBuildTask = p.getTasks().findByName(preBuildTaskName);
            if (preBuildTask != null) {
                generateTask.configure(t -> t.mustRunAfter(preBuildTask));
                processLayoutsTask.configure(t -> t.mustRunAfter(preBuildTask));
            }
        });
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
