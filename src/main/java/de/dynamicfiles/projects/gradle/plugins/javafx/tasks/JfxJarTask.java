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

import com.oracle.tools.packager.Log;
import com.sun.javafx.tools.packager.CreateJarParams;
import com.sun.javafx.tools.packager.PackagerException;
import com.sun.javafx.tools.packager.PackagerLib;
import de.dynamicfiles.projects.gradle.plugins.javafx.JavaFXGradlePluginExtension;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.CopySpec;
import org.gradle.api.specs.Specs;
import org.gradle.api.tasks.TaskAction;

/**
 *
 * @author Danny Althoff
 */
public class JfxJarTask extends JfxTask {

    @TaskAction
    public void jfxjar() {
        Project project = this.getProject();
        // get our configuration
        JavaFXGradlePluginExtension ext = project.getExtensions().getByType(JavaFXGradlePluginExtension.class);
        addDeployDirToSystemClassloader(project, ext.getDeployDir());

        // set logger-level
        Log.setLogger(new Log.Logger(ext.isVerbose()));

        // within maven we would get the jar-content inside some folder BEFORE it is out into JAR-file
        // within gradle we have to extract that folder to get all contents
        org.gradle.api.tasks.bundling.Jar jarTask = (org.gradle.api.tasks.bundling.Jar) project.getTasks().findByName("jar");

        // within maven we would get the jar-content inside some folder BEFORE it is out into JAR-file
        // within gradle we have to extract that jar-file to get all contents inside a folder
        Path someTempDir;
        try{
            someTempDir = Files.createTempDirectory("javafx-gradle-plugin");
        } catch(IOException ex){
            throw new GradleException("Couldn't create temporary folder", ex);
        }
        project.getLogger().info("Extraction generated JAR-file ...");
        project.copy((CopySpec copySpec) -> {
            copySpec.into(someTempDir.toFile());
            copySpec.from(project.zipTree(jarTask.getArchivePath()));
        });

        project.getLogger().info("Creating parameter-map for packager...");

        CreateJarParams createJarParams = new CreateJarParams();
        createJarParams.setOutdir(new File(project.getProjectDir(), ext.getJfxAppOutputDir()));

        // check if we got some filename ending with ".jar"
        if( !ext.getJfxMainAppJarName().toLowerCase().endsWith(".jar") ){
            throw new GradleException("Please provide a proper value for jfxMainAppJarName-property! It has to end with \".jar\".");
        }
        createJarParams.setOutfile(ext.getJfxMainAppJarName());
        createJarParams.setApplicationClass(ext.getMainClass());
        createJarParams.setCss2bin(ext.isCss2bin());
        createJarParams.setPreloader(ext.getPreLoader());

        Map<String, String> manifestAttributes = ext.getManifestAttributes();
        if( manifestAttributes == null ){
            manifestAttributes = new HashMap<>();
        }
        createJarParams.setManifestAttrs(manifestAttributes);

        final File libDir = new File(new File(project.getProjectDir(), ext.getJfxAppOutputDir()), "lib");
        if( !libDir.exists() && !libDir.mkdirs() ){
            throw new GradleException("Unable to create app lib dir: " + libDir);
        }

        if( ext.isUpdateExistingJar() ){
            createJarParams.addResource(null, jarTask.getArchivePath());
        } else {
            // produced and extracted jar-file
            createJarParams.addResource(someTempDir.toFile(), "");
        }

        List<String> foundLibs = new ArrayList<>();

        // copy dependencies
        // got inspiration from: http://opensourceforgeeks.blogspot.de/2015/05/knowing-gradle-dependency-jars-download.html
        Configuration compileConfiguration = project.getConfigurations().getByName("compile");

        project.getLogger().info("Copying defined compile-dependencies...");

        // this woll work for all non-file dependencies
        compileConfiguration.getResolvedConfiguration().getFirstLevelModuleDependencies().forEach(resolvedDep -> {
            // TODO add dependency-filter
            resolvedDep.getAllModuleArtifacts().forEach(artifact -> {
                try{
                    Path artifactPath = artifact.getFile().toPath();
                    String artifactFileName = artifactPath.getFileName().toString();
                    Files.copy(artifactPath, libDir.toPath().resolve(artifactFileName), StandardCopyOption.REPLACE_EXISTING);
                    // will only append, when everything went right
                    foundLibs.add(artifactFileName);
                } catch(IOException ex){
                    project.getLogger().warn("Couldn't copy dependency " + artifact.getId().getComponentIdentifier().toString(), ex);
                }
            });
        });

        project.getLogger().info("Copying defined compile-dependency-files...");
        // inside "getFiles" all non-maven dependencies (like packager.jar) will be available
        compileConfiguration.getResolvedConfiguration().getFiles(Specs.SATISFIES_ALL).forEach(someFile -> {
            try{
                Path artifactPath = someFile.toPath();
                String artifactFileName = artifactPath.getFileName().toString();

                /*
                // Please install at least Java 1.8u40 for using this feature.
                
                dependencies {
                    compile files("${System.properties['java.home']}/../lib/packager.jar")
                }
                 */
                if( "packager.jar".equals(artifactFileName) && !ext.isAddPackagerJar() ){
                    this.getLogger().info("Skipped adding packager.jar.");
                    return;
                }

                // add this lib only, when not already present (could happen on file-dependencies ... which behaves different from maven-model)
                if( !foundLibs.contains(artifactFileName) ){
                    Files.copy(artifactPath, libDir.toPath().resolve(artifactFileName), StandardCopyOption.REPLACE_EXISTING);
                    foundLibs.add(artifactFileName);
                }
            } catch(IOException ex){
                project.getLogger().warn("Couldn't copy dependency " + someFile.getName(), ex);
            }
        });

        if( !foundLibs.isEmpty() ){
            createJarParams.setClasspath("lib/" + String.join(" lib/", foundLibs));
        }

        // https://docs.oracle.com/javase/8/docs/technotes/guides/deploy/manifest.html#JSDPG896
        if( ext.isAllPermissions() ){
            manifestAttributes.put("Permissions", "all-permissions");
        }

        PackagerLib packagerLib = new PackagerLib();
        try{
            project.getLogger().info("Running packager...");
            packagerLib.packageAsJar(createJarParams);
        } catch(PackagerException ex){
            throw new GradleException("Unable to build JFX JAR for application", ex);
        }

        // cleanup
        if( libDir.list().length == 0 ){
            project.getLogger().info("Deleting unused lib-folder...");
            // remove lib-folder, when nothing ended up there
            libDir.delete();
        }

        // cleanup gradle-temp-folder
        // http://www.adam-bien.com/roller/abien/entry/java_7_deleting_recursively_a
        try{
            Files.walkFileTree(someTempDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }

            });
        } catch(IOException iOException){
            // ignored
        }
    }

}
