Setup for generating some installer with file associations
==========================================================

Calling `gradle jfxNative` will generate some executable jar-file with javafx-support and will
generate some executable file able to run on your machine/achitecture, which is called native launcher.
Depending on the tools you have installed, there will be some installers generated too.

You need to set some `vendor`-name which is required for generating installers.

For every system you target, you need some working system of that targeted one, because the generated result
is generated for each architecture and the operating system using the local installed tool-sets. The used
java(fx)packager is just a wrapper around these tools and the javafx-gradle-plugin is just a wrapper of that
provided by the OpenJDK/OracleJDK.

You can find the JavaFX-JAR-file at `build/jfx/app/project-jfx.jar` and you native launcher can be found at
`build/jfx/native/file-associations/` while the installers reside below the `build/jfx/native/`-folder.

When installing, there are some file association registered on your system (when supported). Double-clicking
these files will start your application having that file as argument. To have this working, you need to specify
at least some extensions and some mime-type (called contentType). For further adjustments you can set some
file description and some special icon, which needs to be the same file-format supported on your target system.

Please read the official documentation of Oracle about the tools you are required to install for having
installers be generated.
