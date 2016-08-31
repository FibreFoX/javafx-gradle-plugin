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
package de.dynamicfiles.projects.gradle.plugins.javafx.tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.function.Consumer;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

/**
 *
 * @author Danny Althoff
 */
public abstract class JfxTask extends DefaultTask {

    protected void addDeployDirToSystemClassloader(Project project, String deployDir) {
        // add deployDir to system classpath
        if( deployDir != null ){

            File targetDeployDir = new File(project.getProjectDir(), deployDir);
            if( !targetDeployDir.exists() ){
                project.getLogger().info("Adding 'deploy' directory wasn't successful, because it does not exist! (" + targetDeployDir.getAbsolutePath() + ").");
                project.getLogger().info("You only need this directory when you want to override some resources.");
                return;
            }
            project.getLogger().info("Adding 'deploy' directory to classpath: " + deployDir);
            URLClassLoader sysloader = (URLClassLoader) this.getClass().getClassLoader();
            Class<URLClassLoader> sysclass = URLClassLoader.class;
            try{
                Method method = sysclass.getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
                method.invoke(sysloader, targetDeployDir.toURI().toURL());
            } catch(NoSuchMethodException | SecurityException | MalformedURLException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex){
                throw new GradleException("Error, could not add URL to system classloader", ex);
            }
        }
    }

    protected boolean isGradleDaemonMode() {
        String javaCommand = System.getProperty("sun.java.command");
        return javaCommand != null && javaCommand.startsWith("org.gradle.launcher.daemon");
    }

    protected void redirectIO(Process p, Logger logger) {
        // when being on daemon-mode, we have to pipe it to the logger
        // @see https://github.com/FibreFoX/javafx-gradle-plugin/issues/29
        new StreamGobbler(p.getInputStream(), consumeInputLine -> {
            logger.lifecycle(consumeInputLine);
        }).run();
        new StreamGobbler(p.getErrorStream(), consumeInputLine -> {
            logger.lifecycle(consumeInputLine);
        }).run();
    }

    // http://stackoverflow.com/a/33386692/1961102
    protected class StreamGobbler implements Runnable {

        private InputStream inputStream;
        private Consumer<String> consumeInputLine;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumeInputLine) {
            this.inputStream = inputStream;
            this.consumeInputLine = consumeInputLine;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumeInputLine);
        }
    }

    protected String getEnvironmentRelativeExecutablePath(boolean useEnvironmentRelativeExecutables) {
        if( useEnvironmentRelativeExecutables ){
            return "";
        }

        String jrePath = System.getProperty("java.home");
        String jdkPath = jrePath + File.separator + ".." + File.separator + "bin" + File.separator;

        return jdkPath;
    }
}
