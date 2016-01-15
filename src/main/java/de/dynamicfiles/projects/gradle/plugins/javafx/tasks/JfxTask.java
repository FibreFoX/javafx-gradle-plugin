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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;

/**
 *
 * @author Danny Althoff
 */
public abstract class JfxTask extends DefaultTask {

    protected void addDeployDirToSystemClassloader(Project project, String deployDir) {
        // add deployDir to system classpath
        if( deployDir != null ){
            if( !new File(deployDir).exists() ){
                project.getLogger().warn("Adding 'deploy' directory wasn't successful, because it does not exist! (" + new File(deployDir).getAbsolutePath() + ")");
                return;
            }
            project.getLogger().info("Adding 'deploy' directory to classpath: " + deployDir);
            URLClassLoader sysloader = (URLClassLoader) this.getClass().getClassLoader();
            Class<URLClassLoader> sysclass = URLClassLoader.class;
            try{
                Method method = sysclass.getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
                method.invoke(sysloader, new File(deployDir).toURI().toURL());
            } catch(NoSuchMethodException | SecurityException | MalformedURLException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex){
                throw new GradleException("Error, could not add URL to system classloader", ex);
            }
        }
    }
}
