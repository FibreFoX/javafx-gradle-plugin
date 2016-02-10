Release Notes
=============

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