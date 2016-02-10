[![Build Status](https://travis-ci.org/FibreFoX/javafx-gradle-plugin.svg?branch=master)](https://travis-ci.org/FibreFoX/javafx-gradle-plugin)
[![Maven Central](https://img.shields.io/maven-central/v/de.dynamicfiles.projects.gradle.plugins/javafx-gradle-plugin.svg)](https://maven-badges.herokuapp.com/maven-central/de.dynamicfiles.projects.gradle.plugins/javafx-gradle-plugin)

# JavaFX-Gradle-Plugin

In the need of some equivalent of the [javafx-maven-plugin](https://github.com/javafx-maven-plugin/javafx-maven-plugin) just for gradle, this project was born. A lot of you might have used the [`javafx-gradle`-plugin from Danno Ferrin](https://bitbucket.org/shemnon/javafx-gradle/), but he decided [to not continue](https://bitbucket.org/shemnon/javafx-gradle/issues/47/adding-manifest-attribute-javafx#comment-24360784) that project.

I've never got green with gradle myself, but that didn't withhold me from doing some research and finally I tinkered this tiny gradle-plugin.

As this is my very first productive contact with gradle, please file separate issues so discuss the current solution.

# Example `build.gradle`

Please adjust your parameters accordingly:

```groovy
buildscript {
    dependencies {
        classpath group: 'de.dynamicfiles.projects.gradle.plugins', name: 'javafx-gradle-plugin', version: '1.2'
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
    mainClass = 'com.something.cool.MainApp'
    appName = 'project'
    manifestAttributes = [
        someElement: "hello world"
    ]
    jfxMainAppJarName = 'myProject.jar'
    vendor = "some serious business corp."
    bundleArguments = [
        // dont bundle JRE (not recommended, but increases build-size/-speed)
        runtime: null
    ]
    // to only use ONE bundler, you can specify it by name
    // bundler = "windows.app"
    secondaryLaunchers = [[appName:"somethingDifferent"], [appName:"somethingDifferent2"]]
}
```

To create jfx-jar just call `gradle jfxJar`, for creating native bundle just call `gradle jfxNative`.


Last Release Notes
==================

**Version 1.2 (10-Feb-2016)**

New:
* added workaround for issue #12 regarding [file descriptor leak inside the JDK starting from 1.8.0_60](https://bugs.openjdk.java.net/browse/JDK-8148717)

As there seems to be no good way for having something like maven-invoker-plugin (which is used for the javafx-maven-plugin), I still need to find a nice way having buildable example-projects.


(Not yet) Release(d) Notes
==================

upcoming Version 1.3 (???-2016)
*(nothing changed yet)*