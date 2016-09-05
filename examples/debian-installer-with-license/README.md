Debain Installer with License
=============================

Creating installers with licenses needs some special adjustments. On Debian-systems, the bundler requires
the license to be in text-file format to include it into the installer routines.

Just make sure to have that text-file being available inside the folder specified via
`additionalAppResources`- property, and please make sure it is not inside any subfolder as the bundler
does not look into them correctly.

You need to specify some license-type aswell for having the `.deb`-builder and `.rpm`-builder working
correct, please make sure to use some known string for the license-type. The Fedora-project has a list
online to take from: https://fedoraproject.org/wiki/Licensing:Main?rd=Licensing#Good_Licenses


```groovy
jfx {
    // ...
    additionalAppResources = 'src/main/additionalFiles'
    bundleArguments = [
        licenseType: 'ASL 2.0',
        licenseFile: 'LICENSE'
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
