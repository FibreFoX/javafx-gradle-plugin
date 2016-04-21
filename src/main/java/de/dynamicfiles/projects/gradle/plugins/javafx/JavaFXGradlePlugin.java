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

import de.dynamicfiles.projects.gradle.plugins.javafx.tasks.JfxGenerateKeystoreTask;
import de.dynamicfiles.projects.gradle.plugins.javafx.tasks.JfxNativeTask;
import de.dynamicfiles.projects.gradle.plugins.javafx.tasks.JfxJarTask;
import de.dynamicfiles.projects.gradle.plugins.javafx.tasks.JfxRunTask;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 *
 * @author Danny Althoff
 */
public class JavaFXGradlePlugin implements Plugin<Project> {

    private static final String ANT_JAVAFX_JAR_FILENAME = "ant-javafx.jar";

    @Override
    public void apply(Project project) {
        // ugly hack by adding ant-javafx-jar for only require to apply javafx-gradle-plugin
        // ... can't change via expected way: dependencies.add("classpath", jfxAntJar)
        // https://discuss.gradle.org/t/how-to-bootstrapp-buildscript-classpath-cannot-change-configuration-classpath-after-it-has-been-resolved/7442
        addJavaFXAntJARToGradleBuildpath(project);

        // gradle is lame, so replace existing tasks with MY NAMES ! *battle-cry*
        JfxJarTask jarTask = project.getTasks().replace("jfxJar", JfxJarTask.class);
        JfxNativeTask nativeTask = project.getTasks().replace("jfxNative", JfxNativeTask.class);
        JfxGenerateKeystoreTask generateKeystoreTask = project.getTasks().replace("jfxGenerateKeyStore", JfxGenerateKeystoreTask.class);
        JfxRunTask runTask = project.getTasks().replace("jfxRun", JfxRunTask.class);

        String taskGroupName = "JavaFX";

        // this is for description
        jarTask.setGroup(taskGroupName);
        jarTask.setDescription("Create executable JavaFX-jar");

        nativeTask.setGroup(taskGroupName);
        nativeTask.setDescription("Create native JavaFX-bundle");

        generateKeystoreTask.setGroup(taskGroupName);
        generateKeystoreTask.setDescription("Create a Java keystore");

        runTask.setGroup(taskGroupName);
        runTask.setDescription("Start generated JavaFX-jar");

        // create jfx-jar only after jar-file was created (is this the right way?!?)
        if( project.getTasks().findByName("jar") == null ){
            throw new GradleException("Could not find jar-task. Please make sure you are applying the 'java'-plugin.");
        }
        jarTask.dependsOn(project.getTasks().getByName("jar"));

        // always create jfx-jar before creating native launcher/bundle
        // (in maven I had to implement a lifecycle for this ... mehhh)
        nativeTask.dependsOn(jarTask);

        // to run our jfx-jar, we have to create it first ;)
        runTask.dependsOn(jarTask);

        // extend project-model to get our settings/configuration via nice configuration
        project.getExtensions().create("jfx", JavaFXGradlePluginExtension.class);
    }

    private void addJavaFXAntJARToGradleBuildpath(Project project) {
        String jfxAntJarPath = "/../lib/" + ANT_JAVAFX_JAR_FILENAME;

        // on java 9, we have a different path
        String javaVersion = System.getProperty("java.version");
        if( javaVersion.startsWith("1.9") || javaVersion.startsWith("9.") ){
            jfxAntJarPath = "/lib/" + ANT_JAVAFX_JAR_FILENAME;
        }

        File jfxAntJar = new File(System.getProperty("java.home") + jfxAntJarPath);

        if( !jfxAntJar.exists() ){
            throw new GradleException("Couldn't find Ant-JavaFX-library, please make sure you've installed some JDK which includes JavaFX (e.g. OracleJDK or OpenJDK and OpenJFX), and JAVA_HOME is set properly.");
        }

        ClassLoader buildscriptClassloader = project.getBuildscript().getClassLoader();

        URLClassLoader sysloader;
        // when running inside IDE or while executing java-tests, this should be handled ;)
        if( "org.gradle.internal.classloader.CachingClassLoader".equals(buildscriptClassloader.getClass().getName()) ){
            // project.getBuildscript().getDependencies().add("classpath", project.files(jfxAntJar));
            // would result into: org.gradle.api.InvalidUserDataException: Cannot change dependencies of configuration ':classpath' after it has been resolved.

            // lets mess with the classloaders...
            sysloader = (URLClassLoader) this.getClass().getClassLoader();
        } else {
            sysloader = (URLClassLoader) buildscriptClassloader;
        }

        // add ant-javafx.jar to the classloader (using a different way as javafx-maven-plugin ;D)
        try{
            List<URL> antJarList = new ArrayList<URL>();
            antJarList.add(jfxAntJar.toURI().toURL());
            // I really don't know, why there isn't a direct way to add some File... or just one URL,
            // but: no need to check if jar already was added ;) it's done inside
            org.gradle.internal.classloader.ClasspathUtil.addUrl(sysloader, antJarList);
        } catch(MalformedURLException ex){
            throw new GradleException("Could not add Ant-JavaFX-JAR to plugin-classloader", ex);
        }
    }

}
