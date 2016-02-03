# JavaFX-Gradle-Plugin

In the need of some equivalent of the [javafx-maven-plugin](https://github.com/javafx-maven-plugin/javafx-maven-plugin) just for gradle, this project was born. A lot of you might have used the [`javafx-gradle`-plugin from Danno Ferrin](https://bitbucket.org/shemnon/javafx-gradle/), but he decided [to not continue](https://bitbucket.org/shemnon/javafx-gradle/issues/47/adding-manifest-attribute-javafx#comment-24360784) that project.

I've never got green with gradle myself, but that didn't withhold me from doing some research and finally I tinkered this tiny gradle-plugin.

As this is my very first productive contact with gradle, please file separate issues so discuss the current solution.

Example `build.gradle`, please adjust your parameters accordingly:

```groovy
buildscript {
    dependencies {
        classpath group: 'de.dynamicfiles.projects.gradle.plugins', name: 'javafx-gradle-plugin', version: '1.1'
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
    compile files("${System.properties['java.home']}/../lib/packager.jar")
}

apply plugin: 'javafx-gradle-plugin'

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
    // bundler= "windows.app"
    secondaryLaunchers = [[appName:"somethingDifferent"], [appName:"somethingDifferent2"]]
}
```

Last Release Notes
==================

**Version 1.1 (28-Jan-2016)**

Bugfixes:
* fixed project-relative path-problems mostly regarding multi-module projects

New:
* added support for gradle daemon-mode

Knowh bugs:
* on windows: when calling task jfxNative you can't call task clean, because there is a possible file descriptor leak (see issue #12)

There will be some examples with the next updates/releases, but this is a spare-time project so please just try it out, not yet recommended for production.


(Not yet) Release(d) Notes
==================

upcoming Version 1.2 (??-2016)
* added workaround for issue #12 regarding [file descriptor leak inside the JDK starting from 1.8.0_60](https://bugs.openjdk.java.net/browse/JDK-8148717)
