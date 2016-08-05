Setup for generating some installer with additional application files
=====================================================================

Calling `gradle jfxNative` will generate some executable jar-file with javafx-support and will
generate some executable file able to run on your machine/achitecture, which is called native launcher.
Depending on the tools you have installed, there will be some installers generated too.

You need to set some `vendor`-name which is required for generating installers.

For every system you target, you need some working system of that targeted one, because the generated result
is generated for each architecture and the operating system using the local installed tool-sets. The used
java(fx)packager is just a wrapper around these tools and the javafx-gradle-plugin is just a wrapper of that
provided by the OpenJDK/OracleJDK.

You can find the JavaFX-JAR-file at `build/jfx/app/project-jfx.jar` and you native launcher can be found at
`build/jfx/native/additional-application-files/` while the installers reside below the `build/jfx/native/`-folder.

To have additional files bundled, create some folder and set additionalAppResources to it's coordinates.
This feature might come comfortable for having some README-files or anything else aside of your application.

Please read the official documentation of Oracle about the tools you are required to install for having
installers be generated.
