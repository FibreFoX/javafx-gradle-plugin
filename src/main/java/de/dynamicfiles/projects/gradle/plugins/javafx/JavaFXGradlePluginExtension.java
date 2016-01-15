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

import java.util.List;
import java.util.Map;

/**
 *
 * @author Danny Althoff
 */
public class JavaFXGradlePluginExtension {

    // AbstractJfxToolsMojo
    private boolean verbose = false;
    private String mainClass = null;
    private String jfxAppOutputDir = "build/jfx/app";
    private String jfxMainAppJarName = "project-jfx.jar";
    private String deployDir = "src/main/deploy";

    // JarMojo
    private boolean css2bin = false;
    private String preLoader = null;
    private boolean updateExistingJar = false;
    private boolean allPermissions = false;
    private Map<String, String> manifestAttributes = null;
    private boolean addPackagerJar = true;
    // private List<Dependency> classpathExcludes = new ArrayList<>();
    // private boolean classpathExcludesTransient = true;

    // NativeMojo
    private String identifier = null;
    private String vendor = null;
    private String nativeOutputDir = "build/jfx/native";
    private String bundler = "ALL";
    private Map<String, String> jvmProperties = null;
    private List<String> jvmArgs = null;
    private Map<String, String> userJvmArgs = null;
    private List<String> launcherArguments = null;
    private String nativeReleaseVersion = "1.0";
    private boolean needShortcut = false;
    private boolean needMenu = false;
    private Map<String, String> bundleArguments = null;
    private String appName = null;
    private String additionalAppResources = null;
    private boolean skipNativeLauncherWorkaround124 = false;
    private boolean skipNativeLauncherWorkaround167 = false;
    private List<Map<String,Object>> secondaryLaunchers = null;
    private List<Map<String,String>> fileAssociations = null;

    /*
        generated methods below
     */
    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public String getJfxAppOutputDir() {
        return jfxAppOutputDir;
    }

    public void setJfxAppOutputDir(String jfxAppOutputDir) {
        this.jfxAppOutputDir = jfxAppOutputDir;
    }

    public String getJfxMainAppJarName() {
        return jfxMainAppJarName;
    }

    public void setJfxMainAppJarName(String jfxMainAppJarName) {
        this.jfxMainAppJarName = jfxMainAppJarName;
    }

    public String getDeployDir() {
        return deployDir;
    }

    public void setDeployDir(String deployDir) {
        this.deployDir = deployDir;
    }

    public boolean isCss2bin() {
        return css2bin;
    }

    public void setCss2bin(boolean css2bin) {
        this.css2bin = css2bin;
    }

    public String getPreLoader() {
        return preLoader;
    }

    public void setPreLoader(String preLoader) {
        this.preLoader = preLoader;
    }

    public boolean isUpdateExistingJar() {
        return updateExistingJar;
    }

    public void setUpdateExistingJar(boolean updateExistingJar) {
        this.updateExistingJar = updateExistingJar;
    }

    public boolean isAllPermissions() {
        return allPermissions;
    }

    public void setAllPermissions(boolean allPermissions) {
        this.allPermissions = allPermissions;
    }

    public Map<String, String> getManifestAttributes() {
        return manifestAttributes;
    }

    public void setManifestAttributes(Map<String, String> manifestAttributes) {
        this.manifestAttributes = manifestAttributes;
    }

    public boolean isAddPackagerJar() {
        return addPackagerJar;
    }

    public void setAddPackagerJar(boolean addPackagerJar) {
        this.addPackagerJar = addPackagerJar;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getNativeOutputDir() {
        return nativeOutputDir;
    }

    public void setNativeOutputDir(String nativeOutputDir) {
        this.nativeOutputDir = nativeOutputDir;
    }

    public String getBundler() {
        return bundler;
    }

    public void setBundler(String bundler) {
        this.bundler = bundler;
    }

    public Map<String, String> getJvmProperties() {
        return jvmProperties;
    }

    public void setJvmProperties(Map<String, String> jvmProperties) {
        this.jvmProperties = jvmProperties;
    }

    public List<String> getJvmArgs() {
        return jvmArgs;
    }

    public void setJvmArgs(List<String> jvmArgs) {
        this.jvmArgs = jvmArgs;
    }

    public Map<String, String> getUserJvmArgs() {
        return userJvmArgs;
    }

    public void setUserJvmArgs(Map<String, String> userJvmArgs) {
        this.userJvmArgs = userJvmArgs;
    }

    public List<String> getLauncherArguments() {
        return launcherArguments;
    }

    public void setLauncherArguments(List<String> launcherArguments) {
        this.launcherArguments = launcherArguments;
    }

    public String getNativeReleaseVersion() {
        return nativeReleaseVersion;
    }

    public void setNativeReleaseVersion(String nativeReleaseVersion) {
        this.nativeReleaseVersion = nativeReleaseVersion;
    }

    public boolean isNeedShortcut() {
        return needShortcut;
    }

    public void setNeedShortcut(boolean needShortcut) {
        this.needShortcut = needShortcut;
    }

    public boolean isNeedMenu() {
        return needMenu;
    }

    public void setNeedMenu(boolean needMenu) {
        this.needMenu = needMenu;
    }

    public Map<String, String> getBundleArguments() {
        return bundleArguments;
    }

    public void setBundleArguments(Map<String, String> bundleArguments) {
        this.bundleArguments = bundleArguments;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAdditionalAppResources() {
        return additionalAppResources;
    }

    public void setAdditionalAppResources(String additionalAppResources) {
        this.additionalAppResources = additionalAppResources;
    }

    public boolean isSkipNativeLauncherWorkaround124() {
        return skipNativeLauncherWorkaround124;
    }

    public void setSkipNativeLauncherWorkaround124(boolean skipNativeLauncherWorkaround124) {
        this.skipNativeLauncherWorkaround124 = skipNativeLauncherWorkaround124;
    }


    public boolean isSkipNativeLauncherWorkaround167() {
        return skipNativeLauncherWorkaround167;
    }

    public void setSkipNativeLauncherWorkaround167(boolean skipNativeLauncherWorkaround167) {
        this.skipNativeLauncherWorkaround167 = skipNativeLauncherWorkaround167;
    }

    public List<Map<String, Object>> getSecondaryLaunchers() {
        return secondaryLaunchers;
    }

    public void setSecondaryLaunchers(List<Map<String, Object>> secondaryLaunchers) {
        this.secondaryLaunchers = secondaryLaunchers;
    }

    public List<Map<String, String>> getFileAssociations() {
        return fileAssociations;
    }

    public void setFileAssociations(List<Map<String, String>> fileAssociations) {
        this.fileAssociations = fileAssociations;
    }

}
