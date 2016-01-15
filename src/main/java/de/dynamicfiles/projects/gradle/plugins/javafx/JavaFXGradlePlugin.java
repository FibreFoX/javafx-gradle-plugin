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
package de.dynamicfiles.projects.gradle.plugins.javafx;

import de.dynamicfiles.projects.gradle.plugins.javafx.tasks.JfxNativeTask;
import de.dynamicfiles.projects.gradle.plugins.javafx.tasks.JfxJarTask;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 *
 * @author Danny Althoff
 */
public class JavaFXGradlePlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        // ugly hack by adding ant-javafx-jar for only require to apply javafx-gradle-plugin
        // ... can't change via expected way: dependencies.add("classpath", jfxAntJar)
        // https://discuss.gradle.org/t/how-to-bootstrapp-buildscript-classpath-cannot-change-configuration-classpath-after-it-has-been-resolved/7442
        addJavaFXAntJARToGradleBuildpath(project);

        // gradle is lame, so replace existing tasks with MY NAMES ! *battle-cry*
        JfxJarTask jarTask = project.getTasks().replace("jfxJar", JfxJarTask.class);
        JfxNativeTask nativeTask = project.getTasks().replace("jfxNative", JfxNativeTask.class);

        String taskGroupName = "JavaFX";

        // this is for description
        jarTask.setGroup(taskGroupName);
        jarTask.setDescription("Create executable JavaFX-jar");

        nativeTask.setGroup(taskGroupName);
        nativeTask.setDescription("Create native JavaFX-bundle");

        // create jfx-jar only after jar-file was created (is this the right way?!?)
        if( project.getTasks().findByName("jar") == null ){
            throw new GradleException("Could not find jar-task. Please make sure you are applying the 'java'-plugin.");
        }
        jarTask.dependsOn(project.getTasks().getByName("jar"));

        // always create jfx-jar before creating native launcher/bundle
        // (in maven I had to implement a lifecycle for this ... mehhh)
        nativeTask.dependsOn(jarTask);

        // extend project-model to get our settings/configuration via nice configuration
        project.getExtensions().create("jfx", JavaFXGradlePluginExtension.class);
    }

    private void addJavaFXAntJARToGradleBuildpath(Project project) {
        String jfxAntJarPath = "/../lib/ant-javafx.jar";

        // on java 9, we have a different path
        String javaVersion = System.getProperty("java.version");
        if( javaVersion.startsWith("1.9") || javaVersion.startsWith("9.") ){
            jfxAntJarPath = "/lib/ant-javafx.jar";
        }

        File jfxAntJar = new File(System.getProperty("java.home") + jfxAntJarPath);

        if( !jfxAntJar.exists() ){
            throw new GradleException("Couldn't find Ant-JavaFX-Library, please make sure you have JDK (with JavaFX) installed.");
        }

        ClassLoader buildscriptClassloader = project.getBuildscript().getClassLoader();
        URLClassLoader sysloader = (URLClassLoader) buildscriptClassloader;

        // only add, when not already existing
        boolean alreadyExisting = false;
        for( URL url : sysloader.getURLs() ){
            if( url.toExternalForm().endsWith("ant-javafx.jar") ){
                alreadyExisting = true;
            }
        }
        if( alreadyExisting ){
            return;
        }

        // normal workaround like in javafx-maven-plugin ;)
        Class<URLClassLoader> sysclass = URLClassLoader.class;
        try{
            Method method = sysclass.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            method.invoke(sysloader, jfxAntJar.toURI().toURL());
        } catch(NoSuchMethodException | SecurityException | MalformedURLException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex){
            throw new GradleException("Error, could not add URL to system classloader", ex);
        }
    }

}
