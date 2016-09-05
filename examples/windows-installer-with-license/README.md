Windows Installer with License
==============================

Creating installers with license displayed needs some special adjustments. On Windows-systems,
the bundler requires the license to be in RTF-file-format to include it into the installer routines.

Just make sure to have that RTF-file being available inside the folder specified via
`additionalAppResources`- property, and please make sure it is not inside any subfolder as the bundler
does not look into them correctly.


```groovy
jfx {
    // ...
    additionalAppResources = 'src/main/additionalFiles'
    bundleArguments = [
        runtime: null,
        licenseFile: 'license.rtf'
    ]
    // ...
}
```

For every system you target, you need some working system of that targeted one, because the generated result
is generated for each architecture and the operating system using the local installed tool-sets. The used
java(fx)packager is just a wrapper around these tools and the javafx-gradle-plugin is just a wrapper of that
provided by the OpenJDK/OracleJDK.

You can find the JavaFX-JAR-file at `build/jfx/app/project-jfx.jar` and you native launcher can be found at
`build/jfx/native/windows-installer-with-license/` while the installers reside below the `build/jfx/native/`-folder.

Please read the official documentation of Oracle about the tools you are required to install for having
installers be generated.
