[![Build Status](https://travis-ci.org/FibreFoX/javafx-gradle-plugin.svg?branch=master)](https://travis-ci.org/FibreFoX/javafx-gradle-plugin)
[![Build status](https://ci.appveyor.com/api/projects/status/19tkbde1wrw8mc8h/branch/master?svg=true)](https://ci.appveyor.com/project/FibreFoX/javafx-gradle-plugin/branch/master)
[![Maven Central](https://img.shields.io/maven-central/v/de.dynamicfiles.projects.gradle.plugins/javafx-gradle-plugin.svg)](https://maven-badges.herokuapp.com/maven-central/de.dynamicfiles.projects.gradle.plugins/javafx-gradle-plugin)

JavaFX-Gradle-Plugin
====================

In the need of some equivalent of the [javafx-maven-plugin](https://github.com/javafx-maven-plugin/javafx-maven-plugin) just for gradle, this project was born. A lot of you might have used the [`javafx-gradle`-plugin from Danno Ferrin](https://bitbucket.org/shemnon/javafx-gradle/), but he decided [to not continue](https://bitbucket.org/shemnon/javafx-gradle/issues/47/adding-manifest-attribute-javafx#comment-24360784) that project.

Using javafx-gradle-plugin enhances your build-script with `javapackager`-power. No more using Apache Ant-calls, because this gradle-plugin wraps all calls and introduces workarounds and fixes for not-yet-fixed JDK-bugs.


Example `build.gradle`
======================

Please adjust your parameters accordingly:

```groovy
buildscript {
    dependencies {
        classpath group: 'de.dynamicfiles.projects.gradle.plugins', name: 'javafx-gradle-plugin', version: '8.4.1'
    }
    
    repositories {
        mavenCentral()
    }
}

apply plugin: 'java'

repositories {
    mavenCentral()
}

dependencies{
    // this dependency is only required when using UserJvmOptionsService
    // compile files("${System.properties['java.home']}/../lib/packager.jar")
}

apply plugin: 'javafx-gradle-plugin'

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
    
    skipNativeLauncherWorkaround124 = false
    skipNativeLauncherWorkaround167 = false
    skipJNLPRessourcePathWorkaround182 = false
    skipSigningJarFilesJNLP185 = false
    skipSizeRecalculationForJNLP185 = false
    
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


Gradle Tasks
============

* `gradle jfxJar` - Create executable JavaFX-jar
* `gradle jfxNative` - Create native JavaFX-bundle (will run `jfxJar` first)
* `gradle jfxRun` - Create the JavaFX-jar and runs it like you would do using `java -jar my-project-jfx.jar`
* `gradle jfxGenerateKeyStore` - Create a Java keystore


Last Release Notes
==================

**Version 8.4.1 (15-Mar-2016)**

Bugfixes:
* copy dependencies with runtime-scope (fixes #15)

Starting with this 8.4.x-release I will keep the [javafx-gradle-plugin](https://github.com/FibreFoX/javafx-gradle-plugin) and the [javafx-maven-plugin](https://github.com/javafx-maven-plugin/javafx-maven-plugin) in sync. This means, that you can compare the features of each plugin by comparing its major- and minor-version-number, I'm using [semantic versioning v2](http://semver.org/spec/v2.0.0.html).

Next thing will be to create some tests and example-projects.


(Not yet) Release(d) Notes
==========================

upcoming Version 8.4.2 (???-2016)

New:
* added new property to skip workaround for gradle daemon mode (which causes problems with the runtime-folder, see issue #12 for more information)
