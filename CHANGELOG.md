Release Notes
=============


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