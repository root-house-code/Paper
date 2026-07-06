plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
}

// This project lives under OneDrive, whose sync client transiently locks files
// inside build/ while it scans freshly-written output, intermittently breaking
// incremental Gradle tasks (mergeDebugResources in particular). Build output
// doesn't need to be backed up anyway, so it's redirected outside the synced tree.
val localBuildRoot = File(System.getProperty("user.home"), "AppData/Local/PaperAndroidBuild")
layout.buildDirectory.set(File(localBuildRoot, "root"))
subprojects {
    layout.buildDirectory.set(File(localBuildRoot, name))
}
