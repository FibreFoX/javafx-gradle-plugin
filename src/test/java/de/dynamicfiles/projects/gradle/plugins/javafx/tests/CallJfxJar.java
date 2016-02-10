/*
 * Copyright 2016 Danny Althoff
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.dynamicfiles.projects.gradle.plugins.javafx.tests;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.initialization.dsl.ScriptHandler;
import org.gradle.testfixtures.ProjectBuilder;
import org.testng.annotations.Test;

/**
 *
 * @author Danny Althoff
 */
public class CallJfxJar {

    @Test
    public void callJfxJar() {
        Project project = ProjectBuilder.builder().build();
        DependencyHandler dependencyHandler = project.getDependencies();
        ScriptHandler buildscript = project.getBuildscript();

        RepositoryHandler repositories = buildscript.getRepositories();
        repositories.add(repositories.mavenLocal());

        Dependency pluginDependency = dependencyHandler.create("de.dynamicfiles.projects.gradle.plugins:javafx-gradle-plugin:+");
        buildscript.getDependencies().add("classpath", pluginDependency);
        project.getPluginManager().apply("java");
        project.getPluginManager().apply("javafx-gradle-plugin");
        // TODO
    }

}
