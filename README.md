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
        classpath group: 'de.dynamicfiles.projects.gradle.plugins', name: 'javafx-gradle-plugin', version: '8.5.1'
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
    customBundlers // List<String>
    skipNativeLauncherWorkaround205 = false
    
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

**Version 8.5.1 (29-June-2016)**

New:
* added new property to skip workaround for gradle daemon mode (which causes problems with the runtime-folder, see issue #12 for more information), this makes it now possible to create native builds within the IDE (at least once)

Bugfixes:
* fixed some classloader-problem when using javafx-gradle-plugin in combination with Netbeans IDE having netbeans-gradle-plugin installed *(I'm very sorry about this, don't know why this wasn't detected by me before)*
* updated workaround-detection for creating native bundles without JRE, because [it got fixed by latest Oracle JDK 1.8.0u92](http://www.oracle.com/technetwork/java/javase/2col/8u92-bugfixes-2949473.html)

Enhancements:
* made it possible to specify file-association icon as [String](http://docs.oracle.com/javase/8/docs/api/java/lang/String.html), [File](http://docs.oracle.com/javase/8/docs/api/java/io/File.html) or [Path](http://docs.oracle.com/javase/8/docs/api/java/nio/file/Path.html)
* changed the way for adding `ant-javafx.jar` to the classloaders (by using more stuff provided by the gradle-api)

Migrated from javafx-maven-plugin:
* (bugfix) added workaround for native linux launcher inside native linux installer bundle (DEB and RPM) not working, see issue [#205](https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/205) for more details on this (it's a come-back of the [issue 124](https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/124))
* (new) added ability to write and use custom bundlers! This makes it possible to customize the work which is required for your bundling-process.
* (new) added new property to disable "native linux launcher inside native linux installer"-fix `skipNativeLauncherWorkaround205 = true`
* (improvement) moved workarounds and workaround-detection into its own class (makes it a bit easier to concentrate on the main work inside JfxNativeTask)

**Note:**
There won't be any [GString](http://docs.groovy-lang.org/latest/html/api/groovy/lang/GString.html)-support, please use `toString()` inside your buildscript

Another note: I know, dependency-filtering is not yet implemented, but as this is a rather unused feature, I will take the time ;)



(Not yet) Release(d) Notes
==========================

upcoming Version 8.5.2 (???-2016)

* nothing changed yet