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
        classpath group: 'de.dynamicfiles.projects.gradle.plugins', name: 'javafx-gradle-plugin', version: '8.7.0'
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
    
    // gradle jfxJar
    css2bin = false
    preLoader = null // String
    updateExistingJar = false
    allPermissions = false
    manifestAttributes = null // Map<String, String>
    addPackagerJar = true
    copyAdditionalAppResourcesToJar = false

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
    appName = "project" // this is used for files below "src/main/deploy", e.g. "src/main/deploy/windows/project.ico"
    additionalBundlerResources = null // path to some additional resources for the bundlers when creating application-bundle
    additionalAppResources = null // path to some additional resources when creating application-bundle
    secondaryLaunchers = [[appName:"somethingDifferent"], [appName:"somethingDifferent2"]]
    fileAssociations = null // List<Map<String, Object>>
    noBlobSigning = false // when using bundler "jnlp", you can choose to NOT use blob signing
    customBundlers = null // List<String>
    failOnError = false
    onlyCustomBundlers = false
    skipJNLP = false
    skipNativeVersionNumberSanitizing = false
    
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
        classpath group: 'de.dynamicfiles.projects.gradle.plugins', name: 'javafx-gradle-plugin', version: '8.7.0'
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

Gradle Tasks
============

* `gradle jfxJar` - Create executable JavaFX-jar
* `gradle jfxNative` - Create native JavaFX-bundle (will run `jfxJar` first)
* `gradle jfxRun` - Create the JavaFX-jar and runs it like you would do using `java -jar my-project-jfx.jar`, adjustable using `runJavaParameter`/`runJavaParameter`-parameter
* `gradle jfxGenerateKeyStore` - Create a Java keystore
* `gradle jfxListBundlers` - List all possible bundlers available on this system, use '--info' parameter for detailed information


Using `SNAPSHOT`-versions
=========================
When you report a bug and this got worked around, you might be able to have access to some -SNAPSHOT-version, please adjust your buildscript:

```groovy
buildscript {
    dependencies {
        classpath group: 'de.dynamicfiles.projects.gradle.plugins', name: 'javafx-gradle-plugin', version: '8.7.0-SNAPSHOT'
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

**Version 8.7.0 (09-September-2016)**

New:
* added `checkForAbsolutePaths`-property to enable absolute paths for all path-properties (it defaults to `false` to behave like before)
* added `additionalBundlerResources` for being able to have additional files available to the used bundler
* added feature for copying additionalAppResources to `build/jfx/app` when calling `jfxJar` and `jfxRun`, making it possible to have all that files available (like native files being required to not reside in the jar-files) by setting `copyAdditionalAppResourcesToJar = true` (issue #39)

Bugfixes:
* made it possible to specify absolute paths for all path-properties, fixes issue #36
* reverted the idea of registering the real tasks after project-evaluation, only add ant-javafx.jar after project-evaluation (fixes issue #31)
* adjusted CI-files for AppVeyor and TravisCI to handle functional tests
* fixed possible file-handler leak (unreported)

Changes:
* removed the `skipDaemonModeCheck`-property, please remove this from your configuration/buildscript

Enhancements:
* implemented some functional tests, mostly using the example-projects as test-projects (running against Gradle 2.10 and Gradle 3.0)
* added example project: windows installer with license
* added example project: debian installer with license
* added example project: adjusted launcher-icon
* added example project: additional bundler-files
* extracted plugin-version into separated file to have example-projects working at their place without having the need to adjust these version-numbers on every release
* refactored a bit to have cleaner code



(Not yet) Release(d) Notes
==========================

upcoming Version 8.8.0 (??-feb-2017)

New:
* `nativeReleaseVersion` will now get sanitized, anything than numbers and dots are removed, this ensures compatibility with the used bundler toolsets
* when using `noBlobSigning = true` (which will get dropped with JDK9) the `jarsigner` executable will be used, but it was lacking proper customization, therefor a new property was introduced `additionalJarsignerParameters` which will be appended to all other stuff on the jarsigner-command

Changes:
* reimplemented `additionalBundlerResources`, now searching for folders with the name of the used bundler, makes it possible to adjust nearly all bundlers now (for Mac a special replacement-class was created, as the default one did not provide any way to add more files)

Enhancements:
* updated all example-projects to use a different variable-name of the "current" plugin-version (fixes issue #40)
* added warning about slow performance (even on SSD) when having ext4/btrfs filesystems using "deb"-bundler (fixes issue #41)
* added warning about missing "jnlp.outfile"-property inside bundleArguments when using JNLP-bundler (from issue #42)

Bugfixes:
* added support for Gradle 3.3 (fixes issue #52)

New:
* added ability to fail the build on errors while bundling, just set `failOnError = true` inside the jfx-block
* when having not specified any bundler, it now is possible to remove that JNLP-warning regarding "No OutFile Specificed", which makes that bundler being skipped, just set `skipJNLP = true` inside the jfx-block
* added property to skip `nativeReleaseVersion` rewriting, just set `skipNativeVersionNumberSanitizing = true` inside the jfx-block
