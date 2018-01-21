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
package de.dynamicfiles.projects.gradle.plugins.javafx.tests.exampleprojects;

import de.dynamicfiles.projects.gradle.plugins.javafx.tasks.internal.JavaDetectionTools;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Danny Althoff
 */
public abstract class ExampleProjectTest {

    protected static final List<String> GRADLE_VERSIONS_TO_TEST_AGAINST = new ArrayList<>();

    static {
        if(JavaDetectionTools.IS_JAVA_8){
            GRADLE_VERSIONS_TO_TEST_AGAINST.add("2.10");
            GRADLE_VERSIONS_TO_TEST_AGAINST.add("3.0");
            // this version removed "org.gradle.internal.classloader.ClasspathUtil"
            GRADLE_VERSIONS_TO_TEST_AGAINST.add("3.3");
        }
        GRADLE_VERSIONS_TO_TEST_AGAINST.add("4.4.1");
    }

    protected static String versionString = "+";

    public static void readVersionString() throws IOException {
        List<String> versionFileLines = Files.readAllLines(new File("version.gradle").toPath());
        versionFileLines.forEach(line -> {
            if( line.contains("javafxGradlePluginVersion") ){
                versionString = line.replace("javafxGradlePluginVersion", "").replace("=", "").replace("'", "").trim();
            }
        });
    }

    protected void writePluginVersionIntoBuildScript(Path targetFolder) throws IOException {
        // adjust the inclusion of version.gradle-file
        Path buildScript = targetFolder.resolve("build.gradle");

        Files.write(buildScript, Files.readAllLines(buildScript).stream().map(line -> {
            // remove this, we are "hardcoding" our version
            if( "apply from: '../../version.gradle'".equals(line.trim()) ){
                return "";
            }
            if( line.endsWith("version: \"${gradle.javafxGradlePluginVersion}\"") ){
                return line.replace("version: \"${gradle.javafxGradlePluginVersion}\"", "version: '" + versionString + "'");
            }
            return line;
        }).collect(Collectors.toList()), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
    }

    protected void copyFolderRecursive(Path sourceFolder, Path targetFolder) throws IOException {
        Files.walkFileTree(sourceFolder, new FileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path subfolder, BasicFileAttributes attrs) throws IOException {
                // do create subfolder (if needed)
                Files.createDirectories(targetFolder.resolve(sourceFolder.relativize(subfolder)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path sourceFile, BasicFileAttributes attrs) throws IOException {
                // do copy
                Files.copy(sourceFile, targetFolder.resolve(sourceFolder.relativize(sourceFile)), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path source, IOException ioe) throws IOException {
                // don't fail, just inform user
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
