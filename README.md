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



Example `build.gradle`
======================

Please adjust your parameters accordingly:

```groovy
buildscript {
    dependencies {
        classpath group: 'de.dynamicfiles.projects.gradle.plugins', name: 'javafx-gradle-plugin', version: '8.5.2'
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
jfx {
    verbose = true
    mainClass = "com.something.cool.MainApp"
    jfxAppOutputDir = "build/jfx/app"
    jfxMainAppJarName = "project-jfx.jar"
    deployDir = "src/main/deploy"
    
    // gradle jfxJar
    css2bin = false
    preLoader = null
    updateExistingJar = false
    allPermissions = false
    manifestAttributes = null // Map<String, String>
    addPackagerJar = true

    // gradle jfxNative
    identifier = null // setting this for windows-bundlers makes it possible to generate upgradeable installers (using same GUID)
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
    additionalAppResources = null // path to some additional resources when creating application-bundle
    secondaryLaunchers = [[appName:"somethingDifferent"], [appName:"somethingDifferent2"]]
    fileAssociations = null // List<Map<String, Object>>
    noBlobSigning = false // when using bundler "jnlp", you can choose to NOT use blob signing
    customBundlers // List<String>
    skipNativeLauncherWorkaround205 = false
    
    skipNativeLauncherWorkaround124 = false
    skipNativeLauncherWorkaround167 = false
    skipJNLPRessourcePathWorkaround182 = false
    skipSigningJarFilesJNLP185 = false
    skipSizeRecalculationForJNLP185 = false

    // per default the outcome of the gradle "jarTask" will be used, set this to specify otherwise (like proguard-output)
    alternativePathToJarFile = null // String
    
    // to disable patching of ant-javafx.jar, set this to false
    usePatchedJFXAntLib = true
    
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
```
buildscript {
    dependencies {
        classpath group: 'de.dynamicfiles.projects.gradle.plugins', name: 'javafx-gradle-plugin', version: '8.5.2'
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
* `gradle jfxRun` - Create the JavaFX-jar and runs it like you would do using `java -jar my-project-jfx.jar`
* `gradle jfxGenerateKeyStore` - Create a Java keystore


Using `SNAPSHOT`-versions
=========================
When you report a bug and this got worked around, you might be able to have access to some -SNAPSHOT-version, please adjust your buildscript:

```
buildscript {
    dependencies {
        classpath group: 'de.dynamicfiles.projects.gradle.plugins', name: 'javafx-gradle-plugin', version: '8.6.0-SNAPSHOT'
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

**Version 8.5.2 (31-July-2016)**

Bugfixes:
* fixed issue #24 regarding NullPointerException inside workaround (I'm very sorry about that, thanks to @AustinShalit for finding this)

**Note:**
There won't be any [GString](http://docs.groovy-lang.org/latest/html/api/groovy/lang/GString.html)-support, please use `toString()` inside your buildscript

Another note: I know, dependency-filtering is not yet implemented, but as this is a rather unused feature, I will take the time ;)



(Not yet) Release(d) Notes
==========================

upcoming Version 8.6.0 (??-Aug-2016)

New
* added `alternativePathToJarFile`-property to specify the jar-file which gets used for javafx-jar-transformation
* added `usePatchedJFXAntLib`-property to disable gradle daemon workaround of the JDK-bug (which might be required when patching does result in crashing the JVM)

Bugfixes
* fixed issue #29 and #30 regarding stdout/stderr not printed when Gradle is in daemon mode (which is default for Gradle 3 now)
* fixed issue #12 regarding gradle daemon mode: **IT IS NOW SUPPORTED**

Changes
* removed the usage of `skipDaemonModeCheck`-property, please remove this from your configuration/buildscript (will be removed in the next minor-release)
* the javafx-gradle-plugin now requires ASM being present on classpath of the buildscript for being able to work around the JDK-bug (https://bugs.openjdk.java.net/browse/JDK-8148717)

Enhancements
* fixed issue #26 by providing a way to specify jar-file used for javapackager
* updated proguard example using new `alternativePathToJarFile`-property
* updated README.md to show minimal setup and other stuff