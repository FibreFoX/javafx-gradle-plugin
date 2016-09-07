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
package de.dynamicfiles.projects.gradle.plugins.javafx.dto;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Data transfer object for configuring secondary native launchers.
 *
 * @author Danny Althoff
 */
public class NativeLauncher {

    /**
     * This has to be different than original appname, as all existing parameter are copied and this would be overwritten.
     */
    private String appName = null;
    private String mainClass = null;
    private File jfxMainAppJarName = null;
    private Map<String, String> jvmProperties = null;
    private List<String> jvmArgs = null;
    private Map<String, String> userJvmArgs = null;
    private String nativeReleaseVersion = null;
    private boolean needShortcut;
    private boolean needMenu;
    private String vendor = null;
    private String identifier = null;
    /**
     * To override default generated classpath, set this to your wanted value.
     */
    private String classpath = null;
    private List<String> launcherArguments = null;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public File getJfxMainAppJarName() {
        return jfxMainAppJarName;
    }

    public void setJfxMainAppJarName(File jfxMainAppJarName) {
        this.jfxMainAppJarName = jfxMainAppJarName;
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

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getClasspath() {
        return classpath;
    }

    public void setClasspath(String classpath) {
        this.classpath = classpath;
    }

    public List<String> getLauncherArguments() {
        return launcherArguments;
    }

    public void setLauncherArguments(List<String> launcherArguments) {
        this.launcherArguments = launcherArguments;
    }

}
