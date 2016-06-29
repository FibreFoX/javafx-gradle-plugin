Minimal setup for generating some executable native launcher without bundled JRE
================================================================================

Calling `gradle jfxNative` will generate some executable jar-file with javafx-support and will
generate some executable file able to run on your machine/achitecture, which is called native launcher.
Depending on the tools you have installed, there will be some installers generated too.

You need to set some `vendor`-name which is required for generating installers.

To exclude the normally included/bundled Java Runtime Environment, just set the `runtime` inside
the `bundleArguments` to `null`. This will create smaller installers, but requires your application
to solely rely on the assumption that you end-user has installed the right version of some JRE.

```groovy
jfx {
    // ...
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
`build/jfx/native/minimal-setup-without-bundled-jre/` while the installers reside below the `build/jfx/native/`-folder.

Please read the official documentation of Oracle about the tools you are required to install for having
installers be generated.
