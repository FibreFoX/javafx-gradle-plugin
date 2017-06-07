# How to add native libraries (on Windows) #

Put your .dll on `natives` and add this snipped to `build.gradle`


```
task addNativeLibs(type: Copy) {
    from 'natives'
    into "build/jfx/native/$jfx.appName"
}

release.dependsOn addNativeLibs
```
