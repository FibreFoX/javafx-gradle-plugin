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
import com.sun.javafx.tools.packager.PackagerException;
import com.sun.javafx.tools.packager.PackagerLib;
import com.sun.javafx.tools.packager.SignJarParams;
import de.dynamicfiles.projects.gradle.plugins.javafx.JavaFXGradlePluginExtension;
import de.dynamicfiles.projects.gradle.plugins.javafx.converter.FileAssociation;
import de.dynamicfiles.projects.gradle.plugins.javafx.converter.NativeLauncher;
import de.dynamicfiles.projects.gradle.plugins.javafx.tasks.internal.JavaDetectionTools;
import de.dynamicfiles.projects.gradle.plugins.javafx.tasks.internal.Workarounds;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.TaskAction;

/**
 *
 * @author Danny Althoff
 */
public class JfxNativeTask extends JfxTask {

    private static final String JNLP_JAR_PATTERN = "(.*)href=(\".*?\")(.*)size=(\".*?\")(.*)";

    private Workarounds workarounds = null;

    @TaskAction
    public void jfxnative() {
        Project project = this.getProject();
        // get our configuration
        JavaFXGradlePluginExtension ext = project.getExtensions().getByType(JavaFXGradlePluginExtension.class);
        addDeployDirToSystemClassloader(project, ext.getDeployDir());

        String bundler = ext.getBundler();
        final Logger logger = project.getLogger();

        workarounds = new Workarounds(new File(project.getProjectDir(), ext.getNativeOutputDir()), logger);

        Map<String, ? super Object> params = new HashMap<>();

        logger.info("Creating parameter-map for bundler '" + bundler + "'");

        params.put(StandardBundlerParam.VERBOSE.getID(), ext.isVerbose());
        Optional.ofNullable(ext.getIdentifier()).ifPresent(id -> {
            params.put(StandardBundlerParam.IDENTIFIER.getID(), id);
        });

        // on gradle we don't have nice appnames .... i think?!
        String appName = ext.getAppName();
        if( appName == null ){
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
                .map(appRessourcesString -> new File(project.getProjectDir(), appRessourcesString))
                .filter(File::exists)
                .ifPresent(appResources -> {
                    logger.info("Copying additional app ressources...");
                    try{
                        Path targetFolder = new File(project.getProjectDir(), ext.getJfxAppOutputDir()).toPath();
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
                                logger.warn(String.format("Couldn't copy additional app resource %s with reason %s", source.toString(), ioe.getLocalizedMessage()));
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult postVisitDirectory(Path source, IOException ioe) throws IOException {
                                // nothing to do here
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } catch(IOException e){
                        logger.warn("Couldn't copy additional application resource-file.", e);
                    }
                });

        // adding all resource-files
        Set<File> resourceFiles = new HashSet<>();
        try{
            Files.walk(new File(project.getProjectDir(), ext.getJfxAppOutputDir()).toPath())
                    .map(p -> p.toFile())
                    .filter(File::isFile)
                    .filter(File::canRead)
                    .forEach(f -> {
                        logger.info(String.format("Add %s file to application resources.", f));
                        resourceFiles.add(f);
                    });
        } catch(IOException e){
            logger.warn("There was a problem while processing application files.", e);
        }
        params.put(StandardBundlerParam.APP_RESOURCES.getID(), new RelativeFileSet(new File(project.getProjectDir(), ext.getJfxAppOutputDir()), resourceFiles));

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
            logger.info("Adding configuration for secondary native launcher");
            nullLauncherNameFound.set(launchersMap.stream().map(launcherMap -> getNativeLauncher(launcherMap)).anyMatch(launcher -> launcher.getAppName() == null));
            if( !nullLauncherNameFound.get() ){
                launcherNames.addAll(launchersMap.stream().map(launcherMap -> getNativeLauncher(launcherMap)).map(launcher -> launcher.getAppName()).collect(Collectors.toList()));

                // assume we have valid entry here
                params.put(StandardBundlerParam.SECONDARY_LAUNCHERS.getID(), launchersMap.stream().map(launcherMap -> getNativeLauncher(launcherMap)).map(launcher -> {
                    logger.info("Adding secondary launcher: " + launcher.getAppName());
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

        // create list for file associations (these are very tricky, because the bundlers behave differently)
        Optional.ofNullable(ext.getFileAssociations()).ifPresent(associations -> {
            final List<Map<String, ? super Object>> allAssociations = new ArrayList<>();
            associations.stream().map(associationMap -> getFileAssociation(associationMap)).forEach(association -> {
                Map<String, ? super Object> settings = new HashMap<>();
                settings.put(StandardBundlerParam.FA_DESCRIPTION.getID(), association.getDescription());
                settings.put(StandardBundlerParam.FA_ICON.getID(), association.getIcon());
                settings.put(StandardBundlerParam.FA_EXTENSIONS.getID(), association.getExtensions());
                settings.put(StandardBundlerParam.FA_CONTENT_TYPE.getID(), association.getContentType());
                allAssociations.add(settings);
            });
            params.put(StandardBundlerParam.FILE_ASSOCIATIONS.getID(), allAssociations);
        });

        // bugfix for "bundler not being able to produce native bundle without JRE on windows"
        // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/167
        // this has been fixed and made available since 1.8.0u92:
        // http://www.oracle.com/technetwork/java/javase/2col/8u92-bugfixes-2949473.html
        if( workarounds.isWorkaroundForBug167Needed() ){
            if( !ext.isSkipNativeLauncherWorkaround167() ){
                workarounds.applyWorkaround167(params);
            } else {
                getLogger().info("Skipped workaround for native launcher regarding cfg-file-format.");
            }
        }

        // when running on "--daemon"-mode (which is the default while developing with gradle-supporting IDEs)
        // and the runtime is not set to use "system-jre", there will be a problem when calling the clean-task
        // because of some file descriptor leak which exists since 1.8.0_60
        //
        // for reference
        // https://github.com/FibreFoX/javafx-gradle-plugin/issues/12
        // https://bugs.openjdk.java.net/browse/JDK-8148717
        // http://hg.openjdk.java.net/openjfx/8u40/rt/file/eb264cdc5828/modules/fxpackager/src/main/java/com/oracle/tools/packager/windows/WinAppBundler.java#l319
        // http://hg.openjdk.java.net/openjfx/8u60/rt/file/996511a322b7/modules/fxpackager/src/main/java/com/oracle/tools/packager/windows/WinAppBundler.java#l325
        // http://hg.openjdk.java.net/openjfx/9-dev/rt/file/7cae930f7a19/modules/fxpackager/src/main/java/com/oracle/tools/packager/windows/WinAppBundler.java#l374
        if( isGradleDaemonMode() && !ext.isSkipDaemonModeCheck() && (JavaDetectionTools.IS_JAVA_9 || (JavaDetectionTools.IS_JAVA_8 && JavaDetectionTools.isAtLeastOracleJavaUpdateVersion(60))) ){
            if( !params.containsKey("runtime") || params.get("runtime") != null ){
                logger.lifecycle("Gradle is in daemon-mode, skipped executing bundler, because this would result in some error on clean-task. (JDK-8148717)");
                logger.warn("Aborted jfxNative-task");
                return;
            }
        }
        if( ext.isSkipDaemonModeCheck() ){
            logger.warn("Check for gradle daemon-mode was skipped, you might have some problems while bundling.");
        }

        // run bundlers
        Bundlers bundlers = Bundlers.createBundlersInstance(); // service discovery?

        // don't allow to overwrite existing bundler IDs
        List<String> existingBundlerIds = bundlers.getBundlers().stream().map(existingBundler -> existingBundler.getID()).collect(Collectors.toList());

        Optional.ofNullable(ext.getCustomBundlers()).ifPresent(customBundlerList -> {
            customBundlerList.stream().map(customBundlerClassName -> {
                try{
                    Class<?> customBundlerClass = Class.forName(customBundlerClassName);
                    Bundler newCustomBundler = (Bundler) customBundlerClass.newInstance();
                    // if already existing (or already registered), skip this instance
                    if( existingBundlerIds.contains(newCustomBundler.getID()) ){
                        return null;
                    }
                    return newCustomBundler;
                } catch(ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException ex){
                    logger.warn("There was an exception while creating a new instance of custom bundler: " + customBundlerClassName, ex);
                }
                return null;
            }).filter(customBundler -> customBundler != null).forEach(customBundler -> {
                bundlers.loadBundler(customBundler);
            });
        });

        final String cfgWorkaround205Marker = "cfgWorkaround205Marker";
        final String cfgWorkaround205DoneMarker = cfgWorkaround205Marker + ".done";
        boolean foundBundler = false;
        for( Bundler b : bundlers.getBundlers() ){
            boolean runBundler = true;
            if( bundler != null && !"ALL".equalsIgnoreCase(bundler) && !bundler.equalsIgnoreCase(b.getID()) ){
                // this is not the specified bundler
                runBundler = false;
            }

            // Workaround for native installer bundle not creating working executable native launcher
            // (this is a comeback of issue 124)
            // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/205
            // do run application bundler and put the cfg-file to application resources
            if( System.getProperty("os.name").toLowerCase().startsWith("linux") ){
                if( workarounds.isWorkaroundForBug205Needed() ){
                    // check if special conditions for this are met (not jnlp, but not linux.app too, because there another workaround already works)
                    if( !"jnlp".equalsIgnoreCase(bundler) && !"linux.app".equalsIgnoreCase(bundler) && "linux.app".equalsIgnoreCase(b.getID()) ){
                        if( !ext.isSkipNativeLauncherWorkaround205() ){
                            logger.info("Detected linux application bundler needs to run before installer bundlers are executed.");
                            runBundler = true;
                            params.put(cfgWorkaround205Marker, "true");
                        } else {
                            logger.info("Skipped workaround for native linux installer bundlers.");
                        }
                    }
                }
            }
            if( !runBundler ){
                continue;
            }
            foundBundler = true;

            try{
                Map<String, ? super Object> paramsToBundleWith = new HashMap<>(params);
                if( b.validate(paramsToBundleWith) ){
                    b.execute(paramsToBundleWith, new File(project.getProjectDir(), ext.getNativeOutputDir()));

                    // Workaround for "Native package for Ubuntu doesn't work"
                    // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/124
                    // real bug: linux-launcher from oracle-jdk starting from 1.8.0u40 logic to determine .cfg-filename
                    if( workarounds.isWorkaroundForBug124Needed() ){
                        if( "linux.app".equals(b.getID()) ){
                            logger.info("Applying workaround for oracle-jdk-bug since 1.8.0u40 regarding native linux launcher(s).");
                            if( !ext.isSkipNativeLauncherWorkaround124() ){
                                List<NativeLauncher> nativeLaunchers = new ArrayList<>();

                                // bugfix for #24 "NullPointerException on linux without secondary launchers"
                                Optional.ofNullable(ext.getSecondaryLaunchers()).ifPresent(launchers -> {
                                    nativeLaunchers.addAll(launchers.stream().map(launcherMap -> getNativeLauncher(launcherMap)).collect(Collectors.toList()));
                                });

                                workarounds.applyWorkaround124(appName, nativeLaunchers);
                                // only apply workaround for issue 205 when having workaround for issue 124 active
                                if( Boolean.parseBoolean(String.valueOf(params.get(cfgWorkaround205Marker))) && !Boolean.parseBoolean((String) params.get(cfgWorkaround205DoneMarker)) ){
                                    logger.info("Preparing workaround for oracle-jdk-bug since 1.8.0u40 regarding native linux launcher(s) inside native linux installers.");
                                    workarounds.applyWorkaround205(appName, nativeLaunchers, params);
                                    params.put(cfgWorkaround205DoneMarker, "true");
                                }
                            } else {
                                logger.info("Skipped workaround for native linux launcher(s).");
                            }
                        }
                    }

                    if( "jnlp".equals(b.getID()) ){
                        if( workarounds.isWorkaroundForBug182Needed() ){
                            // Workaround for "JNLP-generation: path for dependency-lib on windows with backslash"
                            // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/182
                            // jnlp-bundler uses RelativeFileSet, and generates system-dependent dividers (\ on windows, / on others)
                            logger.info("Applying workaround for oracle-jdk-bug since 1.8.0u60 regarding jar-path inside generated JNLP-files.");
                            if( !ext.isSkipJNLPRessourcePathWorkaround182() ){
                                workarounds.fixPathsInsideJNLPFiles();
                            } else {
                                logger.info("Skipped workaround for jar-paths jar-path inside generated JNLP-files.");
                            }
                        }

                        // Do sign generated jar-files by calling the packager (this might change in the future,
                        // hopefully when oracle reworked the process inside the JNLP-bundler)
                        // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/185
                        if( workarounds.isWorkaroundForBug185Needed(params) ){
                            logger.info("Signing jar-files referenced inside generated JNLP-files.");
                            if( !ext.isSkipSigningJarFilesJNLP185() ){
                                // JavaFX signing using BLOB method will get dropped on JDK 9: "blob signing is going away in JDK9. "
                                // https://bugs.openjdk.java.net/browse/JDK-8088866?focusedCommentId=13889898#comment-13889898
                                if( !ext.isNoBlobSigning() ){
                                    logger.info("Signing jar-files using BLOB method.");
                                    signJarFilesUsingBlobSigning(ext);
                                } else {
                                    logger.info("Signing jar-files using jarsigner.");
                                    signJarFiles(ext);
                                }
                                workarounds.applyWorkaround185(ext.isSkipSizeRecalculationForJNLP185());
                            } else {
                                logger.info("Skipped signing jar-files referenced inside JNLP-files.");
                            }
                        }
                    }

                }
            } catch(UnsupportedPlatformException e){
                // quietly ignored
            } catch(ConfigException e){
                logger.info("Skipping '" + b.getName() + "' because of configuration error '" + e.getMessage() + "'\nAdvice to fix: " + e.getAdvice());
            } catch(GradleException ex){
                throw new GradleException("Got exception while executing bundler.", ex);
            }
        }
        if( !foundBundler ){
            throw new GradleException("No bundler found for given name " + bundler + ". Please check your configuration.");
        }
    }

    private void addToMapWhenNotNull(Object value, String key, Map<String, Object> map) {
        if( value == null ){
            return;
        }
        map.put(key, value);
    }

    private List<File> getGeneratedJNLPFiles(JavaFXGradlePluginExtension ext) {
        List<File> generatedFiles = new ArrayList<>();
        Project project = this.getProject();

        // try-ressource, because walking on files is lazy, resulting in file-handler left open otherwise
        try(Stream<Path> walkstream = Files.walk(new File(project.getProjectDir(), ext.getNativeOutputDir()).toPath())){
            walkstream.forEach(fileEntry -> {
                File possibleJNLPFile = fileEntry.toFile();
                String fileName = possibleJNLPFile.getName();
                if( fileName.endsWith(".jnlp") ){
                    generatedFiles.add(possibleJNLPFile);
                }
            });
        } catch(IOException ignored){
            // NO-OP
        }

        return generatedFiles;
    }

    private List<String> getJARFilesFromJNLPFiles(JavaFXGradlePluginExtension ext) {
        List<String> jarFiles = new ArrayList<>();
        getGeneratedJNLPFiles(ext).stream().map(jnlpFile -> jnlpFile.toPath()).forEach(jnlpPath -> {
            try{
                List<String> allLines = Files.readAllLines(jnlpPath);
                allLines.stream().filter(line -> line.trim().startsWith("<jar href=")).forEach(line -> {
                    String jarFile = line.replaceAll(JNLP_JAR_PATTERN, "$2");
                    jarFiles.add(jarFile.substring(1, jarFile.length() - 1));
                });
            } catch(IOException ignored){
                // NO-OP
            }
        });
        return jarFiles;
    }

    @SuppressWarnings("unchecked")
    private FileAssociation getFileAssociation(Map<String, Object> rawMap) {
        FileAssociation fileAssociation = new FileAssociation();
        fileAssociation.setDescription((String) rawMap.get("description"));
        fileAssociation.setExtensions((String) rawMap.get("extensions"));
        fileAssociation.setContentType((String) rawMap.get("contentType"));
        if( rawMap.get("icon") != null ){
            if( rawMap.get("icon") instanceof File ){
                fileAssociation.setIcon((File) rawMap.get("icon"));
            } else if( rawMap.get("icon") instanceof Path ){
                fileAssociation.setIcon(((Path) rawMap.get("icon")).toAbsolutePath().toFile());
            } else {
                fileAssociation.setIcon(new File((String) rawMap.get("icon")));
            }
        }
        return fileAssociation;
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

    private void signJarFilesUsingBlobSigning(JavaFXGradlePluginExtension ext) {
        checkSigningConfiguration(ext);

        Project project = this.getProject();
        File keyStore = new File(project.getProjectDir(), ext.getKeyStore());

        SignJarParams signJarParams = new SignJarParams();
        signJarParams.setVerbose(ext.isVerbose());
        signJarParams.setKeyStore(keyStore);
        signJarParams.setAlias(ext.getKeyStoreAlias());
        signJarParams.setStorePass(ext.getKeyStorePassword());
        signJarParams.setKeyPass(ext.getKeyPassword());
        signJarParams.setStoreType(ext.getKeyStoreType());

        File nativeOutputDir = new File(project.getProjectDir(), ext.getNativeOutputDir());

        signJarParams.addResource(nativeOutputDir, ext.getJfxMainAppJarName());

        // add all gathered jar-files as resources so be signed
        getJARFilesFromJNLPFiles(ext).forEach(jarFile -> signJarParams.addResource(nativeOutputDir, jarFile));

        project.getLogger().info("Signing JAR files for webstart bundle");
        try{
            new PackagerLib().signJar(signJarParams);
        } catch(PackagerException ex){
            throw new GradleException("There was a problem while signing JAR files.", ex);
        }
    }

    private void signJarFiles(JavaFXGradlePluginExtension ext) {
        checkSigningConfiguration(ext);

        Project project = this.getProject();
        File nativeOutputDir = new File(project.getProjectDir(), ext.getNativeOutputDir());
        AtomicReference<GradleException> exception = new AtomicReference<>();
        getJARFilesFromJNLPFiles(ext).stream().map(relativeJarFilePath -> new File(nativeOutputDir, relativeJarFilePath)).forEach(jarFile -> {
            try{
                // only sign when there wasn't already some problem
                if( exception.get() == null ){
                    signJar(ext, jarFile.getAbsoluteFile());
                }
            } catch(GradleException ex){
                // rethrow later (same trick is done inside apache-tomee project ;D)
                exception.set(ex);
            }
        });
        if( exception.get() != null ){
            throw exception.get();
        }
    }

    private void checkSigningConfiguration(JavaFXGradlePluginExtension ext) {
        Project project = this.getProject();
        File keyStore = new File(project.getProjectDir(), ext.getKeyStore());
        if( !keyStore.exists() ){
            project.getLogger().lifecycle("Keystore does not exist (expected at: " + keyStore + ")");
            throw new GradleException("Keystore does not exist (expected at: " + keyStore + ")");
        }

        if( ext.getKeyStoreAlias() == null || ext.getKeyStoreAlias().isEmpty() ){
            project.getLogger().lifecycle("A 'keyStoreAlias' is required for signing JARs");
            throw new GradleException("A 'keyStoreAlias' is required for signing JARs");
        }

        if( ext.getKeyStorePassword() == null || ext.getKeyStorePassword().isEmpty() ){
            project.getLogger().lifecycle("A 'keyStorePassword' is required for signing JARs");
            throw new GradleException("A 'keyStorePassword' is required for signing JARs");
        }

        String keyPassword = ext.getKeyPassword();
        if( keyPassword == null ){
            ext.setKeyPassword(ext.getKeyStorePassword());
        }
    }

    private void signJar(JavaFXGradlePluginExtension ext, File jarFile) {
        Project project = this.getProject();
        File keyStore = new File(project.getProjectDir(), ext.getKeyStore());
        List<String> command = new ArrayList<>();
        command.add("jarsigner");
        command.add("-strict");
        command.add("-keystore");
        command.add(keyStore.getAbsolutePath());
        command.add("-storepass");
        command.add(ext.getKeyStorePassword());
        command.add("-keypass");
        command.add(ext.getKeyPassword());
        command.add(jarFile.getAbsolutePath());
        command.add(ext.getKeyStoreAlias());
        if( ext.isVerbose() ){
            command.add("-verbose");
        }

        try{
            ProcessBuilder pb = new ProcessBuilder()
                    .inheritIO()
                    .directory(project.getProjectDir())
                    .command(command);
            Process p = pb.start();
            p.waitFor();
            if( p.exitValue() != 0 ){
                throw new GradleException("Signing jar using jarsigner wasn't successful! Please check build-log.");
            }
        } catch(IOException | InterruptedException ex){
            throw new GradleException("There was an exception while signing jar-file: " + jarFile.getAbsolutePath(), ex);
        }
    }
}
