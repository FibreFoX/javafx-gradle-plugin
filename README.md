[![Build Status](https://travis-ci.org/FibreFoX/javafx-gradle-plugin.svg?branch=master)](https://travis-ci.org/FibreFoX/javafx-gradle-plugin)
[![Build status](https://ci.appveyor.com/api/projects/status/19tkbde1wrw8mc8h/branch/master?svg=true)](https://ci.appveyor.com/project/FibreFoX/javafx-gradle-plugin/branch/master)
[![Maven Central](https://img.shields.io/maven-central/v/de.dynamicfiles.projects.gradle.plugins/javafx-gradle-plugin.svg)](https://maven-badges.herokuapp.com/maven-central/de.dynamicfiles.projects.gradle.plugins/javafx-gradle-plugin)


JavaFX-Gradle-Plugin
====================

Using javafx-gradle-plugin enhances your build-script with `javapackager`-power. No more using Apache Ant-calls, because this gradle-plugin wraps all calls and introduces workarounds and fixes for not-yet-fixed JDK-bugs. This gradle-plugin is a convenient-wrapper for the javapackger, so you have to [visit the official documentation](https://docs.oracle.com/javase/8/docs/technotes/guides/deploy/self-contained-packaging.html#A1324980) to know about the requirements on each operating-system.

**Using OpenJDK?** Please make sure you have **OpenJFX** installed too, as the required JavaFX-parts are separated.


**Using Maven?** Not problem, just [switch to the github-project of the javafx-maven-plugin](https://github.com/javafx-maven-plugin/javafx-maven-plugin).



Why does this gradle-plugin exist?
==================================

In the need of some equivalent of the [javafx-maven-plugin](https://github.com/javafx-maven-plugin/javafx-maven-plugin) just for gradle, this project was born. A lot of you might have used the [`javafx-gradle`-plugin from Danno Ferrin](https://bitbucket.org/shemnon/javafx-gradle/), but he decided [to not continue](https://bitbucket.org/shemnon/javafx-gradle/issues/47/adding-manifest-attribute-javafx#comment-24360784) that project.



Requirements
============
* Gradle 2.10 and above (works on Gradle 3 too)
* Java Developer Kit 8 with at least Update 40



OS-specific requirements
========================
* (Windows) EXE installers: Inno Setup
* (Windows) MSI installers: WiX (at least version 3.7)
* (Linux) DEB installers: dpkg-deb
* (Linux) RPM installers: rpmbuild
* (Mac) DMG installers: hdiutil
* (Mac) PKG installers: pkgbuild



Example `build.gradle`
======================

Please adjust your parameters accordingly:

```groovy
buildscript {
    dependencies {
        classpath group: 'de.dynamicfiles.projects.gradle.plugins', name: 'javafx-gradle-plugin', version: '8.8.2'
    }
    
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

apply plugin: 'java'

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies{
    // this dependency is only required when using UserJvmOptionsService
    // when using Oracle JDK
    // compile files("${System.properties['java.home']}/../lib/packager.jar")
    // when using OpenJFX (Ubuntu), please adjust accordingly
    // compile files("/usr/share/java/openjfx/lib/packager.jar")
}

apply plugin: 'javafx-gradle-plugin'

// these values are the examples and defaults
// you won't need them all

// configure javafx-gradle-plugin
// for all available settings please look at the class "JavaFXGradlePluginExtension"
jfx {
    verbose = true
    mainClass = "com.something.cool.MainApp"
    jfxAppOutputDir = "build/jfx/app"
    jfxMainAppJarName = "project-jfx.jar"
    deployDir = "src/main/deploy"
    useEnvironmentRelativeExecutables = true
    libFolderName = "lib"
    
    // gradle jfxJar
    css2bin = false
    preLoader = null // String
    updateExistingJar = false
    allPermissions = false
    manifestAttributes = null // Map<String, String>
    addPackagerJar = true
    copyAdditionalAppResourcesToJar = false
    skipCopyingDependencies = false
    useLibFolderContentForManifestClasspath = false
    fixedManifestClasspath = null

    // gradle jfxNative
    identifier = null  // String - setting this for windows-bundlers makes it possible to generate upgradeable installers (using same GUID)
    vendor = "some serious business corp."
    nativeOutputDir = "build/jfx/native"
    bundler = "ALL" // set this to some specific, if your don't want all bundlers running, examples "windows.app", "jnlp", ...
    jvmProperties = null // Map<String, String>
    jvmArgs = null // List<String>
    userJvmArgs = null // Map<String, String>
    launcherArguments = null // List<String>
    nativeReleaseVersion = "1.0"
    needShortcut = false
    needMenu = false
    bundleArguments = [
        // dont bundle JRE (not recommended, but increases build-size/-speed)
        runtime: null
    ]
    appName = "project" // this is used for files below "src/main/deploy", e.g. "src/main/deploy/package/windows/project.ico"
    additionalBundlerResources = null // path to some additional resources for the bundlers when creating application-bundle
    additionalAppResources = null // path to some additional resources when creating application-bundle
    secondaryLaunchers = [[appName:"somethingDifferent"], [appName:"somethingDifferent2"]]
    fileAssociations = null // List<Map<String, Object>>
    noBlobSigning = false // when using bundler "jnlp", you can choose to NOT use blob signing
    customBundlers = null // List<String>
    failOnError = false
    onlyCustomBundlers = false
    skipJNLP = false
    skipNativeVersionNumberSanitizing = false // anything than numbers or dots are removed
    additionalJarsignerParameters = null // List<String>
    skipMainClassScanning = false // set to true might increase build-speed
    
    skipNativeLauncherWorkaround124 = false
    skipNativeLauncherWorkaround167 = false
    skipNativeLauncherWorkaround205 = false
    skipJNLPRessourcePathWorkaround182 = false
    skipSigningJarFilesJNLP185 = false
    skipSizeRecalculationForJNLP185 = false
    skipMacBundlerWorkaround = false
    
    // gradle jfxRun
    runJavaParameter = null // String
    runAppParameter = null // String

    // per default the outcome of the gradle "jarTask" will be used, set this to specify otherwise (like proguard-output)
    alternativePathToJarFile = null // String
    
    // to disable patching of ant-javafx.jar, set this to false
    usePatchedJFXAntLib = true
    
    // making it able to support absolute paths, defaults to "false" for maintaining old behaviour
    checkForAbsolutePaths = false
    
    // gradle jfxGenerateKeyStore
    keyStore = "src/main/deploy/keystore.jks"
    keyStoreAlias = "myalias"
    keyStorePassword = "password"
    keyPassword = null // will default to keyStorePassword
    keyStoreType = "jks"
    overwriteKeyStore = false
    
    certDomain = null // required
    certOrgUnit = null // defaults to "none"
    certOrg = null // required
    certState = null // required
    certCountry = null // required
}
```


Minimal setup of `build.gradle`
======================
```groovy
buildscript {
    dependencies {
        classpath group: 'de.dynamicfiles.projects.gradle.plugins', name: 'javafx-gradle-plugin', version: '8.8.2'
    }
    repositories {
        mavenLocal()
        mavenCentral()

    }
}
apply plugin: 'java'

repositories {
    mavenLocal()
    mavenCentral()
}

apply plugin: 'javafx-gradle-plugin'


jfx {
    // minimal requirement for jfxJar-task
    mainClass = 'full.qualified.nameOf.TheMainClass'
    
    // minimal requirement for jfxNative-task
    vendor = 'YourName'
}
```

Customize Icons
===============

To customize the icons used in a native bundle, you have to provide the icons for the appropriate bundle.
The icons must follow the file name convention in order to get picked up.

> Tip: Set the `verbose` setting to true, to log which files are picked up from your deploy directory.

## macOS
Icon location: `src/main/deploy/package/macosx`

In macOS you can provide up to three different icons.
* .app icon
* volume icon
* the background of the window, when opening the dmg volume

The icon file name is depended on your `appName` setting of this plugin.

| Type              | Filename                  |
| :---------------- |:------------------------- |
| .app icon         | \<appName>.icns           |
| volume icon       | \<appName>-volume.icns    |
| volume background | \<appName>-background.png |

The icon sizes should follow the specified sizes.
http://iconhandbook.co.uk/reference/chart/osx/

## Linux
Icon location: `src/main/deploy/package/linux`

For Linux you can provide one icon.

The icon file name is depended on your `appName` setting of this plugin.

| Type              | Filename        |
| :---------------- |:--------------- |
| application icon  | \<appName>.png  |

>Whitespaces in the `appName` will be removed in order to lookup for the icon.
For example a name like 'foo Bar' will lookup for a icon like 'fooBar.png'

## Windows
Icon location: `src/main/deploy/package/windows`

For Windows you can provide two different icons.
* application icon
* setup icon - the icon of the installer

| Type              | Filename                  |
| :---------------- |:------------------------- |
| .exe icon         | \<appName>.ico            |
| setup exe icon    | \<appName>-setup-icon.bmp |


Gradle Tasks
============

* `gradle jfxJar` - Create executable JavaFX-jar
* `gradle jfxNative` - Create native JavaFX-bundle (will run `jfxJar` first)
* `gradle jfxRun` - Create the JavaFX-jar and runs it like you would do using `java -jar my-project-jfx.jar`, adjustable using `runJavaParameter`/`runAppParameter`-parameter
* `gradle jfxGenerateKeyStore` - Create a Java keystore
* `gradle jfxListBundlers` - List all possible bundlers available on this system, use '--info' parameter for detailed information


Using `SNAPSHOT`-versions
=========================
When you report a bug and this got worked around, you might be able to have access to some -SNAPSHOT-version, please adjust your buildscript:

```groovy
buildscript {
    dependencies {
        classpath group: 'de.dynamicfiles.projects.gradle.plugins', name: 'javafx-gradle-plugin', version: '8.8.3-SNAPSHOT'
    }
    repositories {
        mavenLocal()
        mavenCentral()
        maven { url "https://oss.sonatype.org/content/repositories/snapshots" }

    }
}
```


Examples
========

Please look at the [examples-folder](/examples) to see some projects in action.


Last Release Notes
==================

**Version 8.8.2 (09-February-2017)**

Bugfixes:
* fixed `launcherArguments` of secondary launchers not being set correctly (fixes issue #55)


(Not yet) Release(d) Notes
==========================

upcoming Version 8.8.3 (???-2017)

*nothing changed yet*
