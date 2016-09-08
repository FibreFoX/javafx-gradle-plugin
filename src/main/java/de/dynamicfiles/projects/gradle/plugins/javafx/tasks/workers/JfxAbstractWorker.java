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
package de.dynamicfiles.projects.gradle.plugins.javafx.tasks.workers;

import de.dynamicfiles.projects.gradle.plugins.javafx.JavaFXGradlePluginExtension;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Consumer;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

/**
 *
 * @author Danny Althoff
 */
public abstract class JfxAbstractWorker {

    protected void addDeployDirToSystemClassloader(Project project, JavaFXGradlePluginExtension ext) {
        // add deployDir to system classpath
        if( ext.getDeployDir() != null ){

            File targetDeployDir = getAbsoluteOrProjectRelativeFile(project, ext.getDeployDir(), ext.isCheckForAbsolutePaths());
            if( !targetDeployDir.exists() ){
                project.getLogger().info("Adding 'deploy' directory wasn't successful, because it does not exist! (" + targetDeployDir.getAbsolutePath() + ").");
                project.getLogger().info("You only need this directory when you want to override some resources.");
                return;
            }
            project.getLogger().info("Adding 'deploy' directory to classpath: " + ext.getDeployDir());
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

    protected File getAbsoluteOrProjectRelativeFile(Project project, String potentialAbsoluteFilePath, boolean checkForAbsolutePaths) {
        File file = new File(potentialAbsoluteFilePath);
        if( file.isAbsolute() && checkForAbsolutePaths ){
            return file;
        }
        return new File(project.getProjectDir(), potentialAbsoluteFilePath);
    }

    protected void copyRecursive(Path sourceFolder, Path targetFolder, Logger logger) throws IOException {
        Files.walkFileTree(sourceFolder, new FileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path subfolder, BasicFileAttributes attrs) throws IOException {
                // do create subfolder (if needed)
                Files.createDirectories(targetFolder.resolve(sourceFolder.relativize(subfolder)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path sourceFile, BasicFileAttributes attrs) throws IOException {
                // do copy, and replace, as the resource might already be existing
                Files.copy(sourceFile, targetFolder.resolve(sourceFolder.relativize(sourceFile)), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path source, IOException ioe) throws IOException {
                // don't fail, just inform user
                logger.warn(String.format("Couldn't copy resource %s with reason %s", source.toString(), ioe.getLocalizedMessage()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path source, IOException ioe) throws IOException {
                // nothing to do here
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
