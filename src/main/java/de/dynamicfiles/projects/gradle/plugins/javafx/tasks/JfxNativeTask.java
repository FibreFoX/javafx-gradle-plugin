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

import com.oracle.tools.packager.Bundler;
import com.oracle.tools.packager.Bundlers;
import com.oracle.tools.packager.ConfigException;
import com.oracle.tools.packager.RelativeFileSet;
import com.oracle.tools.packager.StandardBundlerParam;
import com.oracle.tools.packager.UnsupportedPlatformException;
import de.dynamicfiles.projects.gradle.plugins.javafx.JavaFXGradlePluginExtension;
import de.dynamicfiles.projects.gradle.plugins.javafx.converter.NativeLauncher;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

/**
 *
 * @author Danny Althoff
 */
public class JfxNativeTask extends JfxTask {

    @TaskAction
    public void jfxnative() {
        Project project = this.getProject();
        // get our configuration
        JavaFXGradlePluginExtension ext = project.getExtensions().getByType(JavaFXGradlePluginExtension.class);
        addDeployDirToSystemClassloader(project, ext.getDeployDir());

        String bundler = ext.getBundler();

        Map<String, ? super Object> params = new HashMap<>();

        project.getLogger().info("Creating parameter-map for bundler '" + bundler + "'");

        params.put(StandardBundlerParam.VERBOSE.getID(), ext.isVerbose());
        Optional.ofNullable(ext.getIdentifier()).ifPresent(id -> {
            params.put(StandardBundlerParam.IDENTIFIER.getID(), id);
        });

        // on gradle we don't have nice appnames .... i think?!
        String appName = ext.getAppName();
        if( ext.getAppName() == null ){
            org.gradle.api.tasks.bundling.Jar jarTask = (org.gradle.api.tasks.bundling.Jar) project.getTasks().findByName("jar");
            String archiveName = jarTask.getArchiveName();
            appName = archiveName.substring(0, archiveName.lastIndexOf("."));
        }
        params.put(StandardBundlerParam.APP_NAME.getID(), appName);

        params.put(StandardBundlerParam.VERSION.getID(), ext.getNativeReleaseVersion());
        if( ext.getVendor() == null ){
            throw new GradleException("You have to set a vendor, which is required for bundlers.");
        }
        params.put(StandardBundlerParam.VENDOR.getID(), ext.getVendor());
        params.put(StandardBundlerParam.SHORTCUT_HINT.getID(), ext.isNeedShortcut());
        params.put(StandardBundlerParam.MENU_HINT.getID(), ext.isNeedMenu());
        params.put(StandardBundlerParam.MAIN_CLASS.getID(), ext.getMainClass());

        Optional.ofNullable(ext.getJvmProperties()).ifPresent(jvmProps -> {
            params.put(StandardBundlerParam.JVM_PROPERTIES.getID(), new HashMap<>(jvmProps));
        });
        Optional.ofNullable(ext.getJvmArgs()).ifPresent(jvmOptions -> {
            params.put(StandardBundlerParam.JVM_OPTIONS.getID(), new ArrayList<>(jvmOptions));
        });
        Optional.ofNullable(ext.getUserJvmArgs()).ifPresent(userJvmOptions -> {
            params.put(StandardBundlerParam.USER_JVM_OPTIONS.getID(), new HashMap<>(userJvmOptions));
        });
        Optional.ofNullable(ext.getLauncherArguments()).ifPresent(arguments -> {
            params.put(StandardBundlerParam.ARGUMENTS.getID(), new ArrayList<>(arguments));
        });
        Optional.ofNullable(ext.getAdditionalAppResources())
                .filter(appRessourcesString -> appRessourcesString != null)
                .map(appRessourcesString -> new File(appRessourcesString))
                .filter(File::exists)
                .ifPresent(appResources -> {
                    project.getLogger().info("Copying additional app ressources...");
                    try{
                        Path targetFolder = new File(ext.getJfxAppOutputDir()).toPath();
                        Path sourceFolder = appResources.toPath();
                        Files.walkFileTree(appResources.toPath(), new FileVisitor<Path>() {

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
                                project.getLogger().warn(String.format("Couldn't copy additional app resource %s with reason %s", source.toString(), ioe.getLocalizedMessage()));
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult postVisitDirectory(Path source, IOException ioe) throws IOException {
                                // nothing to do here
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } catch(IOException e){
                        project.getLogger().warn("Couldn't copy additional application resource-file.", e);
                    }
                });

        // adding all resource-files
        Set<File> resourceFiles = new HashSet<>();
        try{
            Files.walk(new File(ext.getJfxAppOutputDir()).toPath())
                    .map(p -> p.toFile())
                    .filter(File::isFile)
                    .filter(File::canRead)
                    .forEach(f -> {
                        project.getLogger().info(String.format("Add %s file to application resources.", f));
                        resourceFiles.add(f);
                    });
        } catch(IOException e){
            project.getLogger().warn("There was a problem while processing application files.", e);
        }
        params.put(StandardBundlerParam.APP_RESOURCES.getID(), new RelativeFileSet(new File(ext.getJfxAppOutputDir()), resourceFiles));

        Collection<String> duplicateKeys = new HashSet<>();
        Optional.ofNullable(ext.getBundleArguments()).ifPresent(bArguments -> {
            duplicateKeys.addAll(params.keySet());
            duplicateKeys.retainAll(bArguments.keySet());
            params.putAll(bArguments);
        });

        if( !duplicateKeys.isEmpty() ){
            throw new GradleException("The following keys in <bundleArguments> duplicate other settings, please remove one or the other: " + duplicateKeys.toString());
        }

        // check for misconfiguration, requires to be different as this would overwrite primary launcher
        Collection<String> launcherNames = new ArrayList<>();
        launcherNames.add(appName);
        final AtomicBoolean nullLauncherNameFound = new AtomicBoolean(false);
        // check "no launcher names" and gather all names
        Optional.ofNullable(ext.getSecondaryLaunchers()).filter(list -> !list.isEmpty()).ifPresent(launchersMap -> {
            project.getLogger().info("Adding configuration for secondary native launcher");
            nullLauncherNameFound.set(launchersMap.stream().map(launcherMap -> getNativeLauncher(launcherMap)).anyMatch(launcher -> launcher.getAppName() == null));
            if( !nullLauncherNameFound.get() ){
                launcherNames.addAll(launchersMap.stream().map(launcherMap -> getNativeLauncher(launcherMap)).map(launcher -> launcher.getAppName()).collect(Collectors.toList()));

                // assume we have valid entry here
                params.put(StandardBundlerParam.SECONDARY_LAUNCHERS.getID(), launchersMap.stream().map(launcherMap -> getNativeLauncher(launcherMap)).map(launcher -> {
                    project.getLogger().info("Adding secondary launcher: " + launcher.getAppName());
                    Map<String, Object> secondaryLauncher = new HashMap<>();
                    addToMapWhenNotNull(launcher.getAppName(), StandardBundlerParam.APP_NAME.getID(), secondaryLauncher);
                    addToMapWhenNotNull(launcher.getMainClass(), StandardBundlerParam.MAIN_CLASS.getID(), secondaryLauncher);
                    addToMapWhenNotNull(launcher.getJfxMainAppJarName(), StandardBundlerParam.MAIN_JAR.getID(), secondaryLauncher);
                    addToMapWhenNotNull(launcher.getNativeReleaseVersion(), StandardBundlerParam.VERSION.getID(), secondaryLauncher);
                    addToMapWhenNotNull(launcher.getVendor(), StandardBundlerParam.VENDOR.getID(), secondaryLauncher);
                    addToMapWhenNotNull(launcher.getIdentifier(), StandardBundlerParam.IDENTIFIER.getID(), secondaryLauncher);

                    addToMapWhenNotNull(launcher.isNeedMenu(), StandardBundlerParam.MENU_HINT.getID(), secondaryLauncher);
                    addToMapWhenNotNull(launcher.isNeedShortcut(), StandardBundlerParam.SHORTCUT_HINT.getID(), secondaryLauncher);

                    // as we can set another JAR-file, this might be completly different
                    addToMapWhenNotNull(launcher.getClasspath(), StandardBundlerParam.CLASSPATH.getID(), secondaryLauncher);

                    Optional.ofNullable(launcher.getJvmArgs()).ifPresent(jvmOptions -> {
                        secondaryLauncher.put(StandardBundlerParam.JVM_OPTIONS.getID(), new ArrayList<>(jvmOptions));
                    });
                    Optional.ofNullable(launcher.getJvmProperties()).ifPresent(jvmProps -> {
                        secondaryLauncher.put(StandardBundlerParam.JVM_PROPERTIES.getID(), new HashMap<>(jvmProps));
                    });
                    Optional.ofNullable(launcher.getUserJvmArgs()).ifPresent(userJvmOptions -> {
                        secondaryLauncher.put(StandardBundlerParam.USER_JVM_OPTIONS.getID(), new HashMap<>(userJvmOptions));
                    });
                    Optional.ofNullable(launcher.getLauncherArguments()).ifPresent(arguments -> {
                        params.put(StandardBundlerParam.ARGUMENTS.getID(), new ArrayList<>(arguments));
                    });
                    return secondaryLauncher;
                }).collect(Collectors.toList()));
            }
        });

        // check "no launcher names"
        if( nullLauncherNameFound.get() ){
            throw new GradleException("Not all secondary launchers have been configured properly.");
        }
        // check "duplicate launcher names"
        Set<String> duplicateLauncherNamesCheckSet = new HashSet<>();
        launcherNames.stream().forEach(launcherName -> duplicateLauncherNamesCheckSet.add(launcherName));
        if( duplicateLauncherNamesCheckSet.size() != launcherNames.size() ){
            throw new GradleException("Secondary launcher needs to have different name, please adjust appName inside your configuration.");
        }
        /*
        Optional.ofNullable(ext.getFileAssociations()).ifPresent(associations -> {
            final List<Map<String, ? super Object>> allAssociations = new ArrayList<>();
            associations.stream().forEach(association -> {
                Map<String, ? super Object> settings = new HashMap<>();
                settings.put(StandardBundlerParam.FA_DESCRIPTION.getID(), association.getDescription());
                settings.put(StandardBundlerParam.FA_ICON.getID(), association.getIcon());
                settings.put(StandardBundlerParam.FA_EXTENSIONS.getID(), association.getExtensions());
                settings.put(StandardBundlerParam.FA_CONTENT_TYPE.getID(), association.getContentType());
                allAssociations.add(settings);
            });
            params.put(StandardBundlerParam.FILE_ASSOCIATIONS.getID(), allAssociations);
        });
         */

        // bugfix for "bundler not being able to produce native bundle without JRE on windows"
        if( isJavaVersion(8) && isAtLeastOracleJavaUpdateVersion(60) ){
            if( !ext.isSkipNativeLauncherWorkaround167() ){
                if( params.containsKey("runtime") ){
                    project.getLogger().info("Applying workaround for oracle-jdk-bug since 1.8.0u60");
                    // the problem is com.oracle.tools.packager.windows.WinAppBundler within createLauncherForEntryPoint-Method
                    // it does NOT respect runtime-setting while calling "writeCfgFile"-method of com.oracle.tools.packager.AbstractImageBundler
                    // since newer java versions (they added possability to have INI-file-format of generated cfg-file, since 1.8.0_60).
                    // Because we want to have backward-compatibility within java 8, we will use parameter-name as hardcoded string!
                    // Our workaround: use prop-file-format
                    params.put("launcher-cfg-format", "prop");
                }
            } else {
                project.getLogger().info("Skipped workaround for native launcher regarding cfg-file-format.");
            }
        }

        // run bundlers
        Bundlers bundlers = Bundlers.createBundlersInstance(); // service discovery?
        boolean foundBundler = false;
        for( Bundler b : bundlers.getBundlers() ){
            if( bundler != null && !"ALL".equalsIgnoreCase(bundler) && !bundler.equalsIgnoreCase(b.getID()) ){
                // this is not the specified bundler
                continue;
            }
            foundBundler = true;

            try{
                Map<String, ? super Object> paramsToBundleWith = new HashMap<>(params);
                if( b.validate(paramsToBundleWith) ){
                    b.execute(paramsToBundleWith, new File(ext.getNativeOutputDir()));

                    // Workaround for "Native package for Ubuntu doesn't work"
                    // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/124
                    // real bug: linux-launcher from oracle-jdk starting from 1.8.0u40 logic to determine .cfg-filename
                    if( isJavaVersion(8) && isAtLeastOracleJavaUpdateVersion(40) ){
                        if( "linux.app".equals(b.getID()) ){
                            project.getLogger().info("Applying workaround for oracle-jdk-bug since 1.8.0u40");
                            if( !ext.isSkipNativeLauncherWorkaround124() ){
                                // apply on main launcher
                                applyNativeLauncherWorkaround(project, ext.getJfxAppOutputDir(), appName);

                                // check on secondary launchers too
                                if( ext.getSecondaryLaunchers() != null && !ext.getSecondaryLaunchers().isEmpty() ){
                                    ext.getSecondaryLaunchers().stream().map(launcherMap -> getNativeLauncher(launcherMap)).map(launcher -> {
                                        return launcher.getAppName();
                                    }).filter(launcherAppName -> {
                                        // check appName containing any dots (which is the bug)
                                        return launcherAppName.contains(".");
                                    }).forEach(launcherAppname -> {
                                        applyNativeLauncherWorkaround(project, ext.getJfxAppOutputDir(), launcherAppname);
                                    });
                                }

                            } else {
                                project.getLogger().info("Skipped workaround for native linux launcher(s).");
                            }
                        }
                    }

                }
            } catch(UnsupportedPlatformException e){
                // quietly ignored
            } catch(ConfigException e){
                project.getLogger().info("Skipping '" + b.getName() + "' because of configuration error '" + e.getMessage() + "'\nAdvice to fix: " + e.getAdvice());
            }
        }
        if( !foundBundler ){
            throw new GradleException("No bundler found for given name " + bundler + ". Please check your configuration.");
        }
    }

    private boolean isJavaVersion(int oracleJavaVersion) {
        String javaVersion = System.getProperty("java.version");
        return javaVersion.startsWith("1." + oracleJavaVersion);
    }

    private boolean isAtLeastOracleJavaUpdateVersion(int updateNumber) {
        String javaVersion = System.getProperty("java.version");
        String[] javaVersionSplitted = javaVersion.split("_");
        if( javaVersionSplitted.length <= 1 ){
            return false;
        }
        String javaUpdateVersionRaw = javaVersionSplitted[1];
        // NumberFormatException on openjdk (the reported Java version is "1.8.0_45-internal")
        String javaUpdateVersion = javaUpdateVersionRaw.replaceAll("[^\\d]", "");
        return Integer.parseInt(javaUpdateVersion, 10) >= updateNumber;
    }

    private void addToMapWhenNotNull(Object value, String key, Map<String, Object> map) {
        if( value == null ){
            return;
        }
        map.put(key, value);
    }

    private void applyNativeLauncherWorkaround(Project project, String nativeOutputDirString, String appName) {
        // check appName containing any dots
        boolean needsWorkaround = appName.contains(".");
        if( !needsWorkaround ){
            return;
        }
        // rename .cfg-file (makes it able to create running applications again, even within installer)
        String newConfigFileName = appName.substring(0, appName.lastIndexOf("."));
        File nativeOutputDir = new File(nativeOutputDirString);
        Path appPath = nativeOutputDir.toPath().resolve(appName).resolve("app");
        String configfileExtension = ".cfg";
        Path oldConfigFile = appPath.resolve(appName + configfileExtension);
        try{
            Files.move(oldConfigFile, appPath.resolve(newConfigFileName + configfileExtension), StandardCopyOption.ATOMIC_MOVE);
        } catch(IOException ex){
            project.getLogger().warn("Couldn't rename configfile. Please see issue #124 of the javafx-maven-plugin for further details.", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private NativeLauncher getNativeLauncher(Map<String, Object> rawMap) {
        NativeLauncher launcher = new NativeLauncher();
        launcher.setAppName((String) rawMap.get("appName"));
        launcher.setMainClass((String) rawMap.get("mainClass"));
        if( rawMap.get("jfxMainAppJarName") != null ){
            launcher.setJfxMainAppJarName(new File((String) rawMap.get("jfxMainAppJarName")));
        }
        launcher.setJvmProperties((Map<String, String>) rawMap.get("jvmProperties"));
        launcher.setJvmArgs((List<String>) rawMap.get("jvmArgs"));
        launcher.setUserJvmArgs((Map<String, String>) rawMap.get("userJvmArgs"));
        launcher.setNativeReleaseVersion((String) rawMap.get("nativeReleaseVersion"));
        launcher.setNeedShortcut(Boolean.valueOf(String.valueOf(rawMap.get("needShortcut"))));
        launcher.setNeedMenu(Boolean.valueOf(String.valueOf(rawMap.get("needMenu"))));
        launcher.setVendor((String) rawMap.get("vendor"));
        launcher.setIdentifier((String) rawMap.get("identifier"));
        launcher.setClasspath((String) rawMap.get("classpath"));
        launcher.setLauncherArguments((List<String>) rawMap.get("launcherArguments"));
        return launcher;
    }
}
