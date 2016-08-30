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
    private List<Map<String, Object>> secondaryLaunchers = null;
    private List<Map<String, Object>> fileAssociations = null;
    private boolean skipNativeLauncherWorkaround124 = false;
    private boolean skipNativeLauncherWorkaround167 = false;
    private boolean skipJNLPRessourcePathWorkaround182 = false;
    private boolean skipSigningJarFilesJNLP185 = false;
    private boolean skipSizeRecalculationForJNLP185 = false;
    private boolean noBlobSigning = false;
    private List<String> customBundlers = null;
    private boolean skipNativeLauncherWorkaround205 = false;

    // GenerateKeyStoreMojo
    private String keyStore = "src/main/deploy/keystore.jks";
    private String keyStoreAlias = "myalias";
    private String keyStorePassword = "password";
    private String keyPassword = null;
    private String keyStoreType = "jks";

    private boolean overwriteKeyStore = false;
    private String certDomain = null;
    private String certOrgUnit = null;
    private String certOrg = null;
    private String certState = null;
    private String certCountry = null;

    @Deprecated
    private boolean skipDaemonModeCheck = false;
    private String alternativePathToJarFile = null;

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

    public List<Map<String, Object>> getSecondaryLaunchers() {
        return secondaryLaunchers;
    }

    public void setSecondaryLaunchers(List<Map<String, Object>> secondaryLaunchers) {
        this.secondaryLaunchers = secondaryLaunchers;
    }

    public List<Map<String, Object>> getFileAssociations() {
        return fileAssociations;
    }

    public void setFileAssociations(List<Map<String, Object>> fileAssociations) {
        this.fileAssociations = fileAssociations;
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

    public boolean isSkipJNLPRessourcePathWorkaround182() {
        return skipJNLPRessourcePathWorkaround182;
    }

    public void setSkipJNLPRessourcePathWorkaround182(boolean skipJNLPRessourcePathWorkaround182) {
        this.skipJNLPRessourcePathWorkaround182 = skipJNLPRessourcePathWorkaround182;
    }

    public boolean isSkipSigningJarFilesJNLP185() {
        return skipSigningJarFilesJNLP185;
    }

    public void setSkipSigningJarFilesJNLP185(boolean skipSigningJarFilesJNLP185) {
        this.skipSigningJarFilesJNLP185 = skipSigningJarFilesJNLP185;
    }

    public boolean isSkipSizeRecalculationForJNLP185() {
        return skipSizeRecalculationForJNLP185;
    }

    public void setSkipSizeRecalculationForJNLP185(boolean skipSizeRecalculationForJNLP185) {
        this.skipSizeRecalculationForJNLP185 = skipSizeRecalculationForJNLP185;
    }

    public boolean isNoBlobSigning() {
        return noBlobSigning;
    }

    public void setNoBlobSigning(boolean noBlobSigning) {
        this.noBlobSigning = noBlobSigning;
    }

    public List<String> getCustomBundlers() {
        return customBundlers;
    }

    public void setCustomBundlers(List<String> customBundlers) {
        this.customBundlers = customBundlers;
    }

    public boolean isSkipNativeLauncherWorkaround205() {
        return skipNativeLauncherWorkaround205;
    }

    public void setSkipNativeLauncherWorkaround205(boolean skipNativeLauncherWorkaround205) {
        this.skipNativeLauncherWorkaround205 = skipNativeLauncherWorkaround205;
    }

    public String getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(String keyStore) {
        this.keyStore = keyStore;
    }

    public String getKeyStoreAlias() {
        return keyStoreAlias;
    }

    public void setKeyStoreAlias(String keyStoreAlias) {
        this.keyStoreAlias = keyStoreAlias;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    public String getKeyStoreType() {
        return keyStoreType;
    }

    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
    }

    public boolean isOverwriteKeyStore() {
        return overwriteKeyStore;
    }

    public void setOverwriteKeyStore(boolean overwriteKeyStore) {
        this.overwriteKeyStore = overwriteKeyStore;
    }

    public String getCertDomain() {
        return certDomain;
    }

    public void setCertDomain(String certDomain) {
        this.certDomain = certDomain;
    }

    public String getCertOrgUnit() {
        return certOrgUnit;
    }

    public void setCertOrgUnit(String certOrgUnit) {
        this.certOrgUnit = certOrgUnit;
    }

    public String getCertOrg() {
        return certOrg;
    }

    public void setCertOrg(String certOrg) {
        this.certOrg = certOrg;
    }

    public String getCertState() {
        return certState;
    }

    public void setCertState(String certState) {
        this.certState = certState;
    }

    public String getCertCountry() {
        return certCountry;
    }

    public void setCertCountry(String certCountry) {
        this.certCountry = certCountry;
    }

    @Deprecated
    public boolean isSkipDaemonModeCheck() {
        return skipDaemonModeCheck;
    }

    @Deprecated
    public void setSkipDaemonModeCheck(boolean skipDaemonModeCheck) {
        this.skipDaemonModeCheck = skipDaemonModeCheck;
    }

    public String getAlternativePathToJarFile() {
        return alternativePathToJarFile;
    }

    public void setAlternativePathToJarFile(String alternativePathToJarFile) {
        this.alternativePathToJarFile = alternativePathToJarFile;
    }

}
