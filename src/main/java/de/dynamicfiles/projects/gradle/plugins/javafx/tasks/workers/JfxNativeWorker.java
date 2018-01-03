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

import com.oracle.tools.packager.AbstractBundler;
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
import de.dynamicfiles.projects.gradle.plugins.javafx.dto.FileAssociation;
import de.dynamicfiles.projects.gradle.plugins.javafx.dto.NativeLauncher;
import de.dynamicfiles.projects.gradle.plugins.javafx.tasks.internal.ParameterMapEntries;
import de.dynamicfiles.projects.gradle.plugins.javafx.tasks.internal.Workarounds;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

/**
 *
 * @author Danny Althoff
 */
public class JfxNativeWorker extends JfxAbstractWorker {

    private static final String JNLP_JAR_PATTERN = "(.*)href=(\".*?\")(.*)size=(\".*?\")(.*)";

    private static final String CFG_WORKAROUND_MARKER = "cfgWorkaroundMarker";
    private static final String CFG_WORKAROUND_DONE_MARKER = CFG_WORKAROUND_MARKER + ".done";

    private Workarounds workarounds = null;

    public void jfxnative(Project project) {
        // get our configuration
        JavaFXGradlePluginExtension ext = project.getExtensions().getByType(JavaFXGradlePluginExtension.class);
        addDeployDirToSystemClassloader(project, ext);

        String requestedBundler = ext.getBundler();
        final Logger logger = project.getLogger();

        workarounds = new Workarounds(getAbsoluteOrProjectRelativeFile(project, ext.getNativeOutputDir(), ext.isCheckForAbsolutePaths()), logger);

        Map<String, ? super Object> params = new HashMap<>();

        logger.info("Creating parameter-map for bundler '" + requestedBundler + "'");

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
        // replace that value
        if( !ext.isSkipNativeVersionNumberSanitizing() && ext.getNativeReleaseVersion() != null ){
            params.put(StandardBundlerParam.VERSION.getID(), ext.getNativeReleaseVersion().replaceAll("[^\\d.]", ""));
        }

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
                .map(appRessourcesString -> getAbsoluteOrProjectRelativeFile(project, appRessourcesString, ext.isCheckForAbsolutePaths()))
                .filter(File::exists)
                .ifPresent(appResources -> {
                    logger.info("Copying additional app ressources...");
                    try{
                        Path targetFolder = getAbsoluteOrProjectRelativeFile(project, ext.getJfxAppOutputDir(), ext.isCheckForAbsolutePaths()).toPath();
                        Path sourceFolder = appResources.toPath();
                        copyRecursive(sourceFolder, targetFolder, project.getLogger());
                    } catch(IOException e){
                        logger.warn("Couldn't copy additional application resource-file(s).", e);
                    }
                });

        // adding all resource-files
        Set<File> resourceFiles = new HashSet<>();
        try{
            Files.walk(getAbsoluteOrProjectRelativeFile(project, ext.getJfxAppOutputDir(), ext.isCheckForAbsolutePaths()).toPath())
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
        params.put(StandardBundlerParam.APP_RESOURCES.getID(), new RelativeFileSet(getAbsoluteOrProjectRelativeFile(project, ext.getJfxAppOutputDir(), ext.isCheckForAbsolutePaths()), resourceFiles));

        Collection<String> duplicateKeys = new HashSet<>();
        Optional.ofNullable(ext.getBundleArguments()).ifPresent(bArguments -> {
            duplicateKeys.addAll(params.keySet());
            duplicateKeys.retainAll(bArguments.keySet());
            params.putAll(bArguments);
        });

        if( !duplicateKeys.isEmpty() ){
            throw new GradleException("The following keys in <bundleArguments> duplicate other settings, please remove one or the other: " + duplicateKeys.toString());
        }

        if( !ext.isSkipMainClassScanning() ){
            boolean mainClassInsideResourceJarFile = resourceFiles.stream().filter(resourceFile -> resourceFile.toString().endsWith(".jar")).filter(resourceJarFile -> isClassInsideJarFile(ext.getMainClass(), resourceJarFile)).findFirst().isPresent();
            if( !mainClassInsideResourceJarFile ){
                // warn user about missing class-file
                logger.warn(String.format("Class with name %s was not found inside provided jar files!! JavaFX-application might not be working !!", ext.getMainClass()));
            }
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
                    Map<String, Object> secondaryLauncherGenerationMap = new HashMap<>();
                    addToMapWhenNotNull(launcher.getAppName(), StandardBundlerParam.APP_NAME.getID(), secondaryLauncherGenerationMap);
                    addToMapWhenNotNull(launcher.getMainClass(), StandardBundlerParam.MAIN_CLASS.getID(), secondaryLauncherGenerationMap);
                    addToMapWhenNotNull(launcher.getJfxMainAppJarName(), StandardBundlerParam.MAIN_JAR.getID(), secondaryLauncherGenerationMap);
                    addToMapWhenNotNull(launcher.getNativeReleaseVersion(), StandardBundlerParam.VERSION.getID(), secondaryLauncherGenerationMap);
                    addToMapWhenNotNull(launcher.getVendor(), StandardBundlerParam.VENDOR.getID(), secondaryLauncherGenerationMap);
                    addToMapWhenNotNull(launcher.getIdentifier(), StandardBundlerParam.IDENTIFIER.getID(), secondaryLauncherGenerationMap);

                    addToMapWhenNotNull(launcher.isNeedMenu(), StandardBundlerParam.MENU_HINT.getID(), secondaryLauncherGenerationMap);
                    addToMapWhenNotNull(launcher.isNeedShortcut(), StandardBundlerParam.SHORTCUT_HINT.getID(), secondaryLauncherGenerationMap);

                    // as we can set another JAR-file, this might be completly different
                    addToMapWhenNotNull(launcher.getClasspath(), StandardBundlerParam.CLASSPATH.getID(), secondaryLauncherGenerationMap);

                    Optional.ofNullable(launcher.getJvmArgs()).ifPresent(jvmOptions -> {
                        secondaryLauncherGenerationMap.put(StandardBundlerParam.JVM_OPTIONS.getID(), new ArrayList<>(jvmOptions));
                    });
                    Optional.ofNullable(launcher.getJvmProperties()).ifPresent(jvmProps -> {
                        secondaryLauncherGenerationMap.put(StandardBundlerParam.JVM_PROPERTIES.getID(), new HashMap<>(jvmProps));
                    });
                    Optional.ofNullable(launcher.getUserJvmArgs()).ifPresent(userJvmOptions -> {
                        secondaryLauncherGenerationMap.put(StandardBundlerParam.USER_JVM_OPTIONS.getID(), new HashMap<>(userJvmOptions));
                    });
                    Optional.ofNullable(launcher.getLauncherArguments()).ifPresent(arguments -> {
                        secondaryLauncherGenerationMap.put(StandardBundlerParam.ARGUMENTS.getID(), new ArrayList<>(arguments));
                    });
                    return secondaryLauncherGenerationMap;
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
                project.getLogger().info("Skipped workaround for native launcher regarding cfg-file-format.");
            }
        }

        // when running on "--daemon"-mode (which is the default while developing with gradle-supporting IDEs)
        // and the runtime is not set to use "system-jre", there will be a problem when calling the clean-task
        // because of some file descriptor leak which exists since 1.8.0_60
        //
        // this got fixed by me inside monkey-patched javafx-ant jar
        //
        // for reference
        // https://github.com/FibreFoX/javafx-gradle-plugin/issues/12
        // https://bugs.openjdk.java.net/browse/JDK-8148717
        // http://hg.openjdk.java.net/openjfx/8u40/rt/file/eb264cdc5828/modules/fxpackager/src/main/java/com/oracle/tools/packager/windows/WinAppBundler.java#l319
        // http://hg.openjdk.java.net/openjfx/8u60/rt/file/996511a322b7/modules/fxpackager/src/main/java/com/oracle/tools/packager/windows/WinAppBundler.java#l325
        // http://hg.openjdk.java.net/openjfx/9-dev/rt/file/7cae930f7a19/modules/fxpackager/src/main/java/com/oracle/tools/packager/windows/WinAppBundler.java#l374
        //
        // run bundlers
        Bundlers bundlers = Bundlers.createBundlersInstance(); // service discovery?
        Collection<Bundler> loadedBundlers = bundlers.getBundlers();

        // makes it possible to kick out all default bundlers
        if( ext.isOnlyCustomBundlers() ){
            loadedBundlers.clear();
        }

        // don't allow to overwrite existing bundler IDs
        List<String> existingBundlerIds = loadedBundlers.stream().map(existingBundler -> existingBundler.getID()).collect(Collectors.toList());

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
                if( ext.isOnlyCustomBundlers() ){
                    loadedBundlers.add(customBundler);
                } else {
                    bundlers.loadBundler(customBundler);
                }
            });
        });

        boolean foundBundler = false;

        // the new feature for only using custom bundlers made it necessary to check for empty bundlers list
        if( loadedBundlers.isEmpty() ){
            throw new GradleException("There were no bundlers registered. Please make sure to add your custom bundlers as dependency to the bundlescript.");
        }

        for( Bundler b : loadedBundlers ){
            String currentRunningBundlerID = b.getID();

            if( !shouldBundlerRun(requestedBundler, currentRunningBundlerID, ext, logger, params) ){
                continue;
            }

            foundBundler = true;

            try{
                if( ext.getAdditionalBundlerResources() != null && workarounds.isWorkaroundForNativeMacBundlerNeeded(getAbsoluteOrProjectRelativeFile(project, ext.getAdditionalBundlerResources(), ext.isCheckForAbsolutePaths())) ){
                    if( !ext.isSkipMacBundlerWorkaround() ){
                        b = workarounds.applyWorkaroundForNativeMacBundler(b, currentRunningBundlerID, params, getAbsoluteOrProjectRelativeFile(project, ext.getAdditionalBundlerResources(), ext.isCheckForAbsolutePaths()));
                    } else {
                        logger.info("Skipping replacement of the 'mac.app'-bundler. Please make sure you know what you are doing!");
                    }
                }

                Map<String, ? super Object> paramsToBundleWith = new HashMap<>(params);

                if( b.validate(paramsToBundleWith) ){

                    doPrepareBeforeBundling(ext, project, currentRunningBundlerID, logger, paramsToBundleWith);

                    // "jnlp bundler doesn't produce jnlp file and doesn't log any error/warning"
                    // https://github.com/FibreFoX/javafx-gradle-plugin/issues/42
                    // the new jnlp-bundler does not work like other bundlers, you have to provide some bundleArguments-entry :(
                    if( "jnlp".equals(currentRunningBundlerID) && !paramsToBundleWith.containsKey("jnlp.outfile") ){
                        if( ext.isFailOnError() ){
                            throw new GradleException("You missed to specify some bundleArguments-entry, please set 'jnlp.outfile', e.g. using appName.");
                        } else {
                            logger.warn("You missed to specify some bundleArguments-entry, please set 'jnlp.outfile', e.g. using appName.");
                            continue;
                        }
                    }

                    // DO BUNDLE HERE ;) and don't get confused about all the other stuff
                    b.execute(paramsToBundleWith, getAbsoluteOrProjectRelativeFile(project, ext.getNativeOutputDir(), ext.isCheckForAbsolutePaths()));

                    applyWorkaroundsAfterBundling(currentRunningBundlerID, logger, ext, appName, params, project);
                }
            } catch(UnsupportedPlatformException e){
                // quietly ignored
            } catch(ConfigException e){
                if( ext.isFailOnError() ){
                    throw new GradleException("Skipping '" + b.getName() + "' because of configuration error '" + e.getMessage() + "'\nAdvice to fix: " + e.getAdvice());
                } else {
                    logger.info("Skipping '" + b.getName() + "' because of configuration error '" + e.getMessage() + "'\nAdvice to fix: " + e.getAdvice());
                }
            } catch(GradleException ex){
                throw new GradleException("Got exception while executing bundler.", ex);
            }
        }

        if( !foundBundler ){
            throw new GradleException("No bundler found for given name " + requestedBundler + ". Please check your configuration.");
        }
    }

    private void applyWorkaroundsAfterBundling(String currentRunningBundlerID, final Logger logger, JavaFXGradlePluginExtension ext, String appName, Map<String, ? super Object> params, Project project) {
        // Workaround for "Native package for Ubuntu doesn't work"
        // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/124
        // real bug: linux-launcher from oracle-jdk starting from 1.8.0u40 logic to determine .cfg-filename
        if( workarounds.isWorkaroundForBug124Needed() ){
            if( "linux.app".equals(currentRunningBundlerID) ){
                logger.info("Applying workaround for oracle-jdk-bug since 1.8.0u40 regarding native linux launcher(s).");
                if( !ext.isSkipNativeLauncherWorkaround124() ){
                    List<NativeLauncher> nativeLaunchers = new ArrayList<>();

                    // bugfix for #24 "NullPointerException on linux without secondary launchers"
                    Optional.ofNullable(ext.getSecondaryLaunchers()).ifPresent(launchers -> {
                        nativeLaunchers.addAll(launchers.stream().map(launcherMap -> getNativeLauncher(launcherMap)).collect(Collectors.toList()));
                    });

                    workarounds.applyWorkaround124(appName, nativeLaunchers);
                    // only apply workaround for issue 205 when having workaround for issue 124 active
                    if( Boolean.parseBoolean(String.valueOf(params.get(CFG_WORKAROUND_MARKER))) && !Boolean.parseBoolean((String) params.get(CFG_WORKAROUND_DONE_MARKER)) ){
                        logger.info("Preparing workaround for oracle-jdk-bug since 1.8.0u40 regarding native linux launcher(s) inside native linux installers.");
                        workarounds.applyWorkaround205(appName, nativeLaunchers, params);
                        params.put(CFG_WORKAROUND_DONE_MARKER, "true");
                    }
                } else {
                    logger.info("Skipped workaround for native linux launcher(s).");
                }
            }
        }

        if( "jnlp".equals(currentRunningBundlerID) ){
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
                        signJarFilesUsingBlobSigning(project, ext);
                    } else {
                        logger.info("Signing jar-files using jarsigner.");
                        signJarFiles(project, ext);
                    }
                    workarounds.applyWorkaround185(ext.isSkipSizeRecalculationForJNLP185());
                } else {
                    logger.info("Skipped signing jar-files referenced inside JNLP-files.");
                }
            }
        }
    }

    private void doPrepareBeforeBundling(JavaFXGradlePluginExtension ext, Project project, String currentRunningBundlerID, final Logger logger, Map<String, ? super Object> paramsToBundleWith) {
        // copy all files every time a bundler runs, because they might cleanup their folders,
        // but user might have extend existing bundler using same foldername (which would end up deleted/cleaned up)
        // fixes "Make it possible to have additional resources for bundlers"
        // see https://github.com/FibreFoX/javafx-gradle-plugin/issues/38
        if( ext.getAdditionalBundlerResources() != null ){
            boolean skipCopyAdditionalBundlerResources = false;

            // keep previous behaviour
            Path additionalBundlerResources = getAbsoluteOrProjectRelativeFile(project, ext.getAdditionalBundlerResources(), ext.isCheckForAbsolutePaths()).toPath();
            Path resolvedBundlerFolder = additionalBundlerResources.resolve(currentRunningBundlerID);

            logger.info("Found additional bundler resources, trying to copy all files into build root, using:" + additionalBundlerResources.toFile().getAbsolutePath());

            File bundlerImageRoot = AbstractBundler.IMAGES_ROOT.fetchFrom(paramsToBundleWith);
            Path targetFolder = bundlerImageRoot.toPath();
            Path sourceFolder = additionalBundlerResources;

            // new behaviour, use bundler-name as folder-name
            if( Files.exists(resolvedBundlerFolder) ){
                logger.info("Found additional bundler resources for bundler " + currentRunningBundlerID);
                sourceFolder = resolvedBundlerFolder;
                // change behaviour to have more control for all bundlers being inside JDK
                switch(currentRunningBundlerID) {
                    case "windows.app":
                        // no copy required, as we already have "additionalAppResources"
                        skipCopyAdditionalBundlerResources = true;
                        break;
                    case "exe":
                        File exeBundlerFolder = ParameterMapEntries.EXE_IMAGE_DIR.fetchFrom(paramsToBundleWith);
                        targetFolder = exeBundlerFolder.toPath();
                        break;
                    case "msi":
                        File msiBundlerFolder = ParameterMapEntries.MSI_IMAGE_DIR.fetchFrom(paramsToBundleWith);
                        targetFolder = msiBundlerFolder.toPath();
                        break;
                    case "windows.service":
                        // no copy required, as we already have "additionalAppResources"
                        skipCopyAdditionalBundlerResources = true;
                        break;
                    case "mac.app":
                        // custom mac bundler might be used
                        if( ext.isSkipMacBundlerWorkaround() ){
                            logger.warn("The bundler with ID 'mac.app' is not supported, as that bundler does not provide any way to copy additional bundler-files.");
                        }
                        skipCopyAdditionalBundlerResources = true;
                        break;
                    case "mac.appStore":
                        // custom mac bundler might be used
                        if( ext.isSkipMacBundlerWorkaround() ){
                            logger.warn("The bundler with ID 'mac.appStore' is not supported for using 'additionalBundlerResources', as that bundler does not provide any way to copy additional bundler-files.");
                        }
                        skipCopyAdditionalBundlerResources = true;
                        break;
                    case "mac.daemon":
                        // this bundler just deletes everything ... it has no bundlerRoot
                        logger.warn("The bundler with ID 'mac.daemon' is not supported, as that bundler does not provide any way to copy additional bundler-files.");
                        skipCopyAdditionalBundlerResources = true;
                        break;
                    case "dmg":
                        // custom mac bundler might be used
                        if( ext.isSkipMacBundlerWorkaround() ){
                            logger.warn("The bundler with ID 'dmg' is not supported for using 'additionalBundlerResources', as that bundler does not provide any way to copy additional bundler-files.");
                        }
                        skipCopyAdditionalBundlerResources = true;
                        break;
                    case "pkg":
                        // custom mac bundler might be used
                        if( ext.isSkipMacBundlerWorkaround() ){
                            logger.warn("The bundler with ID 'pkg' is not supported for using 'additionalBundlerResources', as that bundler does not provide any way to copy additional bundler-files.");
                        }
                        skipCopyAdditionalBundlerResources = true;
                        break;
                    case "linux.app":
                        // no copy required, as we already have "additionalAppResources"
                        skipCopyAdditionalBundlerResources = true;
                        break;
                    case "deb":
                        File linuxDebBundlerFolder = ParameterMapEntries.DEB_IMAGE_DIR.fetchFrom(paramsToBundleWith);
                        targetFolder = linuxDebBundlerFolder.toPath();
                        break;
                    case "rpm":
                        File linuxRpmBundlerFolder = ParameterMapEntries.RPM_IMAGE_DIR.fetchFrom(paramsToBundleWith);
                        targetFolder = linuxRpmBundlerFolder.toPath();
                        break;
                    default:
                        logger.warn("Unknown bundler-ID found, copying from root of additionalBundlerResources into IMAGES_ROOT.");
                        sourceFolder = additionalBundlerResources;
                        break;
                }
            } else {
                logger.info("No additional bundler resources for bundler " + currentRunningBundlerID + " was found, copying all files instead.");
            }
            if( !skipCopyAdditionalBundlerResources ){
                try{
                    logger.info("Copying additional bundler resources into: " + targetFolder.toFile().getAbsolutePath());
                    copyRecursive(sourceFolder, targetFolder, project.getLogger());
                } catch(IOException e){
                    logger.warn("Couldn't copy additional bundler resource-file(s).", e);
                }
            } else {
                logger.info("Skipped copying additional bundler resources, mostly because this bundler does not need them. You might want to use additionalAppResources. To make sure, check for any warnings printed above this message.");
            }
        }

        // check if we need to inform the user about low performance even on SSD
        // https://github.com/FibreFoX/javafx-gradle-plugin/issues/41
        if( System.getProperty("os.name").toLowerCase().startsWith("linux") && "deb".equals(currentRunningBundlerID) ){
            File generationTarget = getAbsoluteOrProjectRelativeFile(project, ext.getNativeOutputDir(), ext.isCheckForAbsolutePaths());
            AtomicBoolean needsWarningAboutSlowPerformance = new AtomicBoolean(false);
            generationTarget.toPath().getFileSystem().getFileStores().forEach(store -> {
                if( "ext4".equals(store.type()) ){
                    needsWarningAboutSlowPerformance.set(true);
                }
                if( "btrfs".equals(store.type()) ){
                    needsWarningAboutSlowPerformance.set(true);
                }
            });
            if( needsWarningAboutSlowPerformance.get() ){
                logger.lifecycle("This bundler might take some while longer than expected.");
                logger.lifecycle("For details about this, please go to: https://wiki.debian.org/Teams/Dpkg/FAQ#Q:_Why_is_dpkg_so_slow_when_using_new_filesystems_such_as_btrfs_or_ext4.3F");
            }
        }
    }

    /*
     * Sometimes we need to work with some bundler, even if it wasn't requested. This happens when one bundler was selected and we need
     * to work with the outcome of some image-bundler (because that JDK-bundler is faulty).
     */
    private boolean shouldBundlerRun(String requestedBundler, String currentRunningBundlerID, JavaFXGradlePluginExtension ext, final Logger logger, Map<String, ? super Object> params) {
        if( requestedBundler != null && !"ALL".equalsIgnoreCase(requestedBundler) && !requestedBundler.equalsIgnoreCase(currentRunningBundlerID) ){
            // this is not the specified bundler
            return false;
        }

        if( ext.isSkipJNLP() && "jnlp".equalsIgnoreCase(currentRunningBundlerID) ){
            logger.info("Skipped JNLP-bundling as requested.");
            return false;
        }

        boolean runBundler = true;
        // Workaround for native installer bundle not creating working executable native launcher
        // (this is a comeback of issue 124)
        // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/205
        // do run application bundler and put the cfg-file to application resources
        if( System.getProperty("os.name").toLowerCase().startsWith("linux") ){
            if( workarounds.isWorkaroundForBug205Needed() ){
                // check if special conditions for this are met (not jnlp, but not linux.app too, because another workaround already works)
                if( !"jnlp".equalsIgnoreCase(requestedBundler) && !"linux.app".equalsIgnoreCase(requestedBundler) && "linux.app".equalsIgnoreCase(currentRunningBundlerID) ){
                    if( !ext.isSkipNativeLauncherWorkaround205() ){
                        logger.info("Detected linux application bundler ('linux.app') needs to run before installer bundlers are executed.");
                        runBundler = true;
                        params.put(CFG_WORKAROUND_MARKER, "true");
                    } else {
                        logger.info("Skipped workaround for native linux installer bundlers.");
                    }
                }
            }
        }
        return runBundler;
    }

    private void addToMapWhenNotNull(Object value, String key, Map<String, Object> map) {
        if( value == null ){
            return;
        }
        map.put(key, value);
    }

    private List<File> getGeneratedJNLPFiles(Project project, JavaFXGradlePluginExtension ext) {
        List<File> generatedFiles = new ArrayList<>();

        // try-ressource, because walking on files is lazy, resulting in file-handler left open otherwise
        try(Stream<Path> walkstream = Files.walk(getAbsoluteOrProjectRelativeFile(project, ext.getNativeOutputDir(), ext.isCheckForAbsolutePaths()).toPath())){
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

    private List<String> getJARFilesFromJNLPFiles(Project project, JavaFXGradlePluginExtension ext) {
        List<String> jarFiles = new ArrayList<>();
        getGeneratedJNLPFiles(project, ext).stream().map(jnlpFile -> jnlpFile.toPath()).forEach(jnlpPath -> {
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
            } else {
                if( rawMap.get("icon") instanceof Path ){
                    fileAssociation.setIcon(((Path) rawMap.get("icon")).toAbsolutePath().toFile());
                } else {
                    fileAssociation.setIcon(new File((String) rawMap.get("icon")));
                }
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

    private void signJarFilesUsingBlobSigning(Project project, JavaFXGradlePluginExtension ext) {
        checkSigningConfiguration(project, ext);

        File keyStore = getAbsoluteOrProjectRelativeFile(project, ext.getKeyStore(), ext.isCheckForAbsolutePaths());

        SignJarParams signJarParams = new SignJarParams();
        signJarParams.setVerbose(ext.isVerbose());
        signJarParams.setKeyStore(keyStore);
        signJarParams.setAlias(ext.getKeyStoreAlias());
        signJarParams.setStorePass(ext.getKeyStorePassword());
        signJarParams.setKeyPass(ext.getKeyPassword());
        signJarParams.setStoreType(ext.getKeyStoreType());

        File nativeOutputDir = getAbsoluteOrProjectRelativeFile(project, ext.getNativeOutputDir(), ext.isCheckForAbsolutePaths());

        signJarParams.addResource(nativeOutputDir, ext.getJfxMainAppJarName());

        // add all gathered jar-files as resources so be signed
        getJARFilesFromJNLPFiles(project, ext).forEach(jarFile -> signJarParams.addResource(nativeOutputDir, jarFile));

        project.getLogger().info("Signing JAR files for webstart bundle");

        try{
            new PackagerLib().signJar(signJarParams);
        } catch(PackagerException ex){
            throw new GradleException("There was a problem while signing JAR files.", ex);
        }
    }

    private void signJarFiles(Project project, JavaFXGradlePluginExtension ext) {
        checkSigningConfiguration(project, ext);

        File nativeOutputDir = getAbsoluteOrProjectRelativeFile(project, ext.getNativeOutputDir(), ext.isCheckForAbsolutePaths());
        AtomicReference<GradleException> exception = new AtomicReference<>();
        getJARFilesFromJNLPFiles(project, ext).stream().map(relativeJarFilePath -> new File(nativeOutputDir, relativeJarFilePath)).forEach(jarFile -> {
            try{
                // only sign when there wasn't already some problem
                if( exception.get() == null ){
                    signJar(project, ext, jarFile.getAbsoluteFile());
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

    private void checkSigningConfiguration(Project project, JavaFXGradlePluginExtension ext) {
        File keyStore = getAbsoluteOrProjectRelativeFile(project, ext.getKeyStore(), ext.isCheckForAbsolutePaths());
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

    private void signJar(Project project, JavaFXGradlePluginExtension ext, File jarFile) {
        File keyStore = getAbsoluteOrProjectRelativeFile(project, ext.getKeyStore(), ext.isCheckForAbsolutePaths());
        List<String> command = new ArrayList<>();
        command.add(getEnvironmentRelativeExecutablePath(ext.isUseEnvironmentRelativeExecutables()) + "jarsigner");
        command.add("-strict");
        command.add("-keystore");
        command.add(keyStore.getAbsolutePath());
        command.add("-storepass");
        command.add(ext.getKeyStorePassword());
        command.add("-keypass");
        command.add(ext.getKeyPassword());
        command.add(jarFile.getAbsolutePath());
        command.add(ext.getKeyStoreAlias());
        Optional.ofNullable(ext.getAdditionalJarsignerParameters()).ifPresent(jarsignerParameters -> {
            command.addAll(jarsignerParameters);
        });

        if( ext.isVerbose() ){
            command.add("-verbose");
        }

        try{
            ProcessBuilder pb = new ProcessBuilder();
            if( !isGradleDaemonMode() ){
                pb.inheritIO();
            }

            if( ext.isVerbose() ){
                project.getLogger().lifecycle("Running command: " + String.join(" ", command));
            }

            pb.directory(project.getProjectDir())
                    .command(command);
            Process p = pb.start();

            if( isGradleDaemonMode() ){
                redirectIO(p, project.getLogger());
            }

            p.waitFor();
            if( p.exitValue() != 0 ){
                throw new GradleException("Signing jar using jarsigner wasn't successful! Please check build-log.");
            }
        } catch(IOException | InterruptedException ex){
            throw new GradleException("There was an exception while signing jar-file: " + jarFile.getAbsolutePath(), ex);
        }
    }

    private boolean isClassInsideJarFile(String classname, File jarFile) {
        String requestedJarEntryName = classname.replace(".", "/") + ".class";
        try{
            JarFile jarFileToSearchIn = new JarFile(jarFile, false, JarFile.OPEN_READ);
            return jarFileToSearchIn.stream().parallel().filter(jarEntry -> {
                return jarEntry.getName().equals(requestedJarEntryName);
            }).findAny().isPresent();
        } catch(IOException ex){
            // NO-OP
        }
        return false;
    }

}
