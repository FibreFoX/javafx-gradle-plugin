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

import com.oracle.tools.packager.Log;
import de.dynamicfiles.projects.gradle.plugins.javafx.JavaFXGradlePluginExtension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.gradle.api.GradleException;
import org.gradle.api.Project;

/**
 *
 * @author Danny Althoff
 */
public class JfxRunWorker extends JfxAbstractWorker {

    public void jfxrun(Project project) {
        // get our configuration
        JavaFXGradlePluginExtension ext = project.getExtensions().getByType(JavaFXGradlePluginExtension.class);
        addDeployDirToSystemClassloader(project, ext);

        // set logger-level
        Log.setLogger(new Log.Logger(ext.isVerbose()));
        project.getLogger().lifecycle("Running JavaFX Application");

        List<String> command = new ArrayList<>();
        command.add(getEnvironmentRelativeExecutablePath(ext.isUseEnvironmentRelativeExecutables()) + "java");

        Optional.ofNullable(ext.getRunJavaParameter()).ifPresent(runJavaParameter -> {
            if( runJavaParameter.trim().isEmpty() ){
                return;
            }
            command.add(runJavaParameter);
        });

        Optional.ofNullable(ext.getRunJavaParameters()).ifPresent(runJavaParameters -> {
            if( runJavaParameters.isEmpty() ){
                return;
            }
            command.addAll(runJavaParameters);
        });

        command.add("-jar");
        command.add(ext.getJfxMainAppJarName());
        Optional.ofNullable(ext.getRunAppParameter()).ifPresent(runAppParameter -> {
            if( runAppParameter.trim().isEmpty() ){
                return;
            }
            command.add(runAppParameter);
        });

        try{
            ProcessBuilder pb = new ProcessBuilder();
            if( !isGradleDaemonMode() ){
                pb.inheritIO();
            }

            if( ext.isVerbose() ){
                project.getLogger().lifecycle("Running command: " + String.join(" ", command));
            }

            pb.directory(getAbsoluteOrProjectRelativeFile(project, ext.getJfxAppOutputDir(), ext.isCheckForAbsolutePaths()))
                    .command(command);
            Process p = pb.start();

            if( isGradleDaemonMode() ){
                redirectIO(p, project.getLogger());
            }

            p.waitFor();
            if( p.exitValue() != 0 ){
                throw new GradleException("There was an exception while executing JavaFX Application. Please check build-log.");
            }
        } catch(IOException | InterruptedException ex){
            throw new GradleException("There was an exception while executing JavaFX Application.", ex);
        }
    }

}
