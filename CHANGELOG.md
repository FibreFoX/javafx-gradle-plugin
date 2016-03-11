Release Notes
=============

# Version 8.4.0 (11-Mar-2016)

New:
* introduced `jfxRun` task
* introduced `jfxGenerateKeyStore` task
* when creating JNLP-files, your can now choose between Blob Signing (which was introduced since JavaFX but seems has never worked, and will be removed from Java 9) or normal signing done by `jarsigner` by providing the new proverty `noBlobSigning: true`

Improvement:
* added appveyor for building javafx-gradle-plugin on windows

Starting with this release I will keep the [javafx-gradle-plugin](https://github.com/FibreFoX/javafx-gradle-plugin) and the [javafx-maven-plugin](https://github.com/javafx-maven-plugin/javafx-maven-plugin) in sync. This means, that you can compare the features of each plugin by comparing its major- and minor-version-number, I'm using [semantic versioning v2](http://semver.org/spec/v2.0.0.html).

Next thing will be to create some tests and example-projects.



# Version 1.3 (08-Mar-2016)

Bugfixes:
* replace backslash with normal slash within JNLP-files
* add signing-feature for bundler with ID "jnlp" (by setting `"jnlp.allPermisions": true` inside bundleArguments)
* fixed size-attributes within JNLP-files when using bundler with ID "jnlp" and requesting to sign the jars

Next versions will be in sync with the javafx-maven-plugin, mostly to have a better visual sign what feature-set exists. This means to me, that I still have to do some work ;)



# Version 1.2 (10-Feb-2016)

New:
* added workaround for issue #12 regarding [file descriptor leak inside the JDK starting from 1.8.0_60](https://bugs.openjdk.java.net/browse/JDK-8148717)

As there seems to be no good way for having something like maven-invoker-plugin (which is used for the javafx-maven-plugin), I still need to find a nice way having buildable example-projects.



# Version 1.1 (28-Jan-2016)

Bugfixes:
* fixed project-relative path-problems mostly regarding multi-module projects

New:
* added support for gradle daemon-mode

Knowh bugs:
* on windows: when calling task jfxNative you can't call task clean, because there is a possible file descriptor leak (see issue #12)

There will be some examples with the next updates/releases, but this is a spare-time project so please just try it out, not yet recommended for production.



# Version 1.0 (16-Jan-2016) Initial Release

This is the very first release of my javafx-gradle-plugin, and my first official gradle-plugin too.