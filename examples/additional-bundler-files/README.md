Setup for generating some installer with additional bundler files
=================================================================

Creating special customized installer-building scripts is often requested, but providing these files
is not always possible using `src/main/deploy/package`-folder nor using `additionalAppResources`, especially
when these files should not be part of the application resources itself.

All bundlers are using some temporary folder for letting the bundling toolsets work on it, including all
application files, your overriding custom files for bundler scripts and the JRE (if existing, because its optional).
The problem here is, that only file-overriding is possible by placing the named files into `src/main/deploy/`-folder,
having additional files for example to encryption of specialized installer modules (license-checks, system-checks)
seems impossible. This feature was added since 8.7.0 of the javafx-gradle-plugin as part of a e-mail request.

Before the bundlers are executed, all files inside the folder provided via the `additionalBundlerResources`-property
inside your `jfx`-configuration will be copied to the working-folder of the bundler. Each bundler might have its
own subfolder:
* exe-installers on windows: "win-exe.image"
* msi-installers on windows: "win-msi.image"

Please take a look into the source-code for your special bundler, as this might be target of changes


```groovy
jfx {
    // ...
    additionalBundlerResources = 'src/main/deploy/bundlers'
    bundleArguments = [
        runtime: null
    ]
    // ...
}
```

For every system you target, you need some working system of that targeted one, because the generated result
is generated for each architecture and the operating system using the local installed tool-sets. The used
java(fx)packager is just a wrapper around these tools and the javafx-gradle-plugin is just a wrapper of that
provided by the OpenJDK/OracleJDK.

You can find the JavaFX-JAR-file at `build/jfx/app/project-jfx.jar` and you native launcher can be found at
`build/jfx/native/additional-bundler-files/` while the installers reside below the `build/jfx/native/`-folder.

Please read the official documentation of Oracle about the tools you are required to install for having
installers be generated.
