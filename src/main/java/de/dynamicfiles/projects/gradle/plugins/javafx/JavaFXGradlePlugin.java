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
import de.dynamicfiles.projects.gradle.plugins.javafx.tasks.JfxListBundlersTask;
import de.dynamicfiles.projects.gradle.plugins.javafx.tasks.JfxRunTask;
import de.dynamicfiles.projects.gradle.plugins.javafx.tasks.internal.JavaDetectionTools;
import de.dynamicfiles.projects.gradle.plugins.javafx.tasks.internal.MonkeyPatcher;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 *
 * @author Danny Althoff
 */
public class JavaFXGradlePlugin implements Plugin<Project> {

    public static final String ANT_JAVAFX_JAR_FILENAME = "ant-javafx.jar";

    @Override
    public void apply(Project project) {
        // can create jfx-jar only after jar-file was created
        if( project.getTasks().findByName("jar") == null ){
            throw new GradleException("Could not find jar-task. Please make sure you are applying the 'java'-plugin.");
        }
        // gradle is lame, so replace existing tasks with MY NAMES ! *battle-cry*

        // tasks will be available for the buldscript prior full evaluation
        JfxJarTask jarTask = project.getTasks().replace(JfxJarTask.JFX_TASK_NAME, JfxJarTask.class);
        JfxNativeTask nativeTask = project.getTasks().replace(JfxNativeTask.JFX_TASK_NAME, JfxNativeTask.class);
        JfxGenerateKeystoreTask generateKeystoreTask = project.getTasks().replace(JfxGenerateKeystoreTask.JFX_TASK_NAME, JfxGenerateKeystoreTask.class);
        JfxRunTask runTask = project.getTasks().replace(JfxRunTask.JFX_TASK_NAME, JfxRunTask.class);
        JfxListBundlersTask jfxListBundlersTask = project.getTasks().replace(JfxListBundlersTask.JFX_TASK_NAME, JfxListBundlersTask.class);

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

        jfxListBundlersTask.setGroup(taskGroupName);
        jfxListBundlersTask.setDescription("List all possible bundlers available on this system, use '--info' parameter for detailed information");

        jarTask.dependsOn(project.getTasks().getByName("jar"));

        // always create jfx-jar before creating native launcher/bundle
        // (in maven I had to implement a lifecycle for this ... mehhh)
        nativeTask.dependsOn(jarTask);

        // to run our jfx-jar, we have to create it first ;)
        runTask.dependsOn(jarTask);

        // extend project-model to get our settings/configuration via nice configuration
        project.getExtensions().create("jfx", JavaFXGradlePluginExtension.class);

        // adding ant-javafx.jar AFTER evaluation, because otherwise we can't know if the user has choosen to NOT patch ant-javafx.jar (in case it is required)
        project.afterEvaluate(evaluatedProject -> {
            // ugly hack by adding ant-javafx-jar for only require to apply javafx-gradle-plugin
            // ... can't change via expected way: dependencies.add("classpath", jfxAntJar)
            // https://discuss.gradle.org/t/how-to-bootstrapp-buildscript-classpath-cannot-change-configuration-classpath-after-it-has-been-resolved/7442
            addJavaFXAntJARToGradleBuildpath(evaluatedProject);
        });
    }

    private void addJavaFXAntJARToGradleBuildpath(Project project) {
        String jfxAntJarPath = "/../lib/" + ANT_JAVAFX_JAR_FILENAME;

        // on java 9, we have a different path
        if( JavaDetectionTools.IS_JAVA_9 ){
            jfxAntJarPath = "/lib/" + ANT_JAVAFX_JAR_FILENAME;
        }

        // always use ant-javafx.jar from the executing JDK (do not use environment-specific paths)
        // per spec "java.home" points to the JRE: https://docs.oracle.com/javase/tutorial/essential/environment/sysprop.html
        File jfxAntJar = new File(System.getProperty("java.home") + jfxAntJarPath);

        if( !jfxAntJar.exists() ){
            throw new GradleException("Couldn't find Ant-JavaFX-library, please make sure you've installed some JDK which includes JavaFX (e.g. OracleJDK or OpenJDK and OpenJFX), and JAVA_HOME is set properly.");
        }

        // don't use SystemClassloader or current Thread-ClassLoader, as we are not maven here ;)
        ClassLoader buildscriptClassloader = project.getBuildscript().getClassLoader();

        URLClassLoader sysloader;
        // when running inside IDE or while executing java-tests, this should be handled ;)
        if( "org.gradle.internal.classloader.CachingClassLoader".equals(buildscriptClassloader.getClass().getName()) ){
            // project.getBuildscript().getDependencies().add("classpath", project.files(jfxAntJar));
            // would result into: org.gradle.api.InvalidUserDataException: Cannot change dependencies of configuration ':classpath' after it has been resolved.

            // lets mess with the classloaders...
            project.getLogger().debug("Using JavaFXGradlePlugin-class classloader");
            sysloader = (URLClassLoader) this.getClass().getClassLoader();
        } else {
            project.getLogger().debug("Using buildscript classloader");
            sysloader = (URLClassLoader) buildscriptClassloader;
        }

        // add ant-javafx.jar to the classloader (using a different way as javafx-maven-plugin ;D)
        try{
            // I'm very sorry for this ugly condition :(
            boolean usePatchedJar = System.getProperty("os.name").toLowerCase().startsWith("windows") && isGradleDaemonMode() && (JavaDetectionTools.IS_JAVA_9 || (JavaDetectionTools.IS_JAVA_8 && JavaDetectionTools.isAtLeastOracleJavaUpdateVersion(60)));
            boolean usePatchedJFXAntLib = project.getExtensions().getByType(JavaFXGradlePluginExtension.class).isUsePatchedJFXAntLib();

            // check if patched jar is required
            if( usePatchedJar && !usePatchedJFXAntLib ){
                usePatchedJar = false;
                project.getLogger().warn("You disabled the patching (by setting 'usePatchedJFXAntLib'-property to 'false' inside 'jfx'-configuration) of the " + ANT_JAVAFX_JAR_FILENAME + ", please make sure you know about the consequences.");
            }

            // check if already added! otherwise we would include/patch that file multiple times :(
            List<URL> loadedAntJavaFXLibs = Arrays.asList(sysloader.getURLs()).stream().filter(loadedURL -> {
                return loadedURL.toExternalForm().endsWith(ANT_JAVAFX_JAR_FILENAME);
            }).collect(Collectors.toList());

            boolean alreadyLoaded = loadedAntJavaFXLibs.size() > 0;
            boolean workaroundLoaded = loadedAntJavaFXLibs.stream().filter(libURL -> libURL.toExternalForm().contains(MonkeyPatcher.WORKAROUND_DIRECTORY_NAME)).count() > 0;

            project.getLogger().debug("DEBUG > Having " + loadedAntJavaFXLibs.size() + " loadedJars");

            // only when not loaded
            if( alreadyLoaded == false ){
                List<URL> antJarList = new ArrayList<>();
                if( usePatchedJar ){
                    URL patchedJfxAntJar = MonkeyPatcher.getPatchedJfxAntJar();
                    antJarList.add(patchedJfxAntJar);
                    project.getLogger().info("using patched " + ANT_JAVAFX_JAR_FILENAME + ", located at > " + patchedJfxAntJar.toExternalForm());
                } else {
                    antJarList.add(jfxAntJar.toURI().toURL());
                }
                // I really don't know, why there isn't a direct way to add some File... or just one URL,
                // but: no need to check if jar already was added ;) it's done inside
                org.gradle.internal.classloader.ClasspathUtil.addUrl(sysloader, antJarList);
            } else {
                if( !usePatchedJFXAntLib && workaroundLoaded ){
                    project.getLogger().warn("Please restart gradle-daemon! Patched " + ANT_JAVAFX_JAR_FILENAME + " is loaded, but you disabled to patch and use that file.");
                }
            }
        } catch(MalformedURLException ex){
            throw new GradleException("Could not add Ant-JavaFX-JAR to plugin-classloader", ex);
        }
    }

    protected boolean isGradleDaemonMode() {
        String javaCommand = System.getProperty("sun.java.command");
        return javaCommand != null && javaCommand.startsWith("org.gradle.launcher.daemon");
    }

}
