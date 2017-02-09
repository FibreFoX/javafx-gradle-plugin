Release Notes
=============

# Version 8.8.2 (09-February-2017)

Bugfixes:
* fixed `launcherArguments` of secondary launchers not being set correctly (fixes issue #55)



# Version 8.8.1 (06-February-2017)

Bugfixes:
* fixed wrong calculated classpath when scanning the lib-folder was activated



# Version 8.8.0 (05-February-2017)

New:
* `nativeReleaseVersion` will now get sanitized, anything than numbers and dots are removed, this ensures compatibility with the used bundler toolsets
* when using `noBlobSigning = true` (which will get dropped with JDK9) the `jarsigner` executable will be used, but it was lacking proper customization, therefor a new property was introduced `additionalJarsignerParameters` which will be appended to all other stuff on the jarsigner-command
* added ability to fail the build on errors while bundling, just set `failOnError = true` inside the jfx-block
* when having not specified any bundler, it now is possible to remove that JNLP-warning regarding "No OutFile Specificed", which makes that bundler being skipped, just set `skipJNLP = true` inside the jfx-block
* added property to skip `nativeReleaseVersion` rewriting, just set `skipNativeVersionNumberSanitizing = true` inside the jfx-block
* added `skipCopyingDependencies` to make it possible to NOT copying dependencies, but they are added to the classpath inside the manifest like normal
* added `fixedManifestClasspath` for setting the classpath-entry inside the generated manifest-file in the main jfx-jar, this is already possible for secondary launchers by setting `classpath` within the configuration-block of the secondary launcher
* added `useLibFolderContentForManifestClasspath` for creating the manifest-entriy for the classpath, depending on the content of the lib-folder, makes it possible to have files not being inside dependencies being present there (which got copied beforehand)

Changes:
* reimplemented `additionalBundlerResources`, now searching for folders with the name of the used bundler, makes it possible to adjust nearly all bundlers now (for Mac a special replacement-class was created, as the default one did not provide any way to add more files)

Enhancements:
* updated all example-projects to use a different variable-name of the "current" plugin-version (fixes issue #40)
* added warning about slow performance (even on SSD) when having ext4/btrfs filesystems using "deb"-bundler (fixes issue #41)
* added warning about missing "jnlp.outfile"-property inside bundleArguments when using JNLP-bundler (from issue #42)
* added ability to change name of the lib-folder by setting `libFolderName`

Bugfixes:
* added support for Gradle 3.3 (fixes issue #52)



# Version 8.7.0 (09-September-2016)

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



# Version 8.6.0 (31-August-2016)

New:
* added `alternativePathToJarFile`-property to specify the jar-file which gets used for javafx-jar-transformation
* added `usePatchedJFXAntLib`-property to disable gradle daemon workaround of the JDK-bug (which might be required when patching does result in crashing the JVM)
* added `useEnvironmentRelativeExecutables`-property to change the executables being used from the JDK instead of environment-relative (which could differ due to multiple local java-installations)
* added possibility to adjust java-command used for `jfxRun`-task: it is now possible to pass parameters to your jfx-application
* added possibility to adjust java-command used for `jfxRun`-task: it is now possible to pass parameters to the java-executable, e.g. to specify some javassist-module or other JVM-related stuff (like Xmx or other funny things)

Bugfixes:
* fixed issue #29 and #30 regarding stdout/stderr not printed when Gradle is in daemon mode (which is default for Gradle 3 now)
* fixed issue #12 regarding gradle daemon mode: **IT IS NOW SUPPORTED**

Changes:
* removed the usage of `skipDaemonModeCheck`-property, please remove this from your configuration/buildscript (will be removed in the next minor-release)
* the javafx-gradle-plugin now requires ASM being present on classpath of the buildscript for being able to work around the JDK-bug (https://bugs.openjdk.java.net/browse/JDK-8148717)

Enhancements:
* fixed issue #26 by providing a way to specify jar-file used for javapackager
* updated proguard example using new `alternativePathToJarFile`-property
* updated README.md to show minimal setup and other stuff



# Version 8.5.2 (31-July-2016)

Bugfixes:
* fixed issue #24 regarding NullPointerException inside workaround (I'm very sorry about that, thanks to @AustinShalit for finding this)

**Note:**
There won't be any [GString](http://docs.groovy-lang.org/latest/html/api/groovy/lang/GString.html)-support, please use `toString()` inside your buildscript

Another note: I know, dependency-filtering is not yet implemented, but as this is a rather unused feature, I will take the time ;)



# Version 8.5.1 (29-June-2016)

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



# Version 8.4.1 (15-Mar-2016)

Bugfixes:
* copy dependencies with runtime-scope (fixes #15)



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
