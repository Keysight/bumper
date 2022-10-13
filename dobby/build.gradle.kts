plugins {
    kotlin("jvm") version "1.7.20" apply false
    kotlin("plugin.serialization") version "1.7.20" apply false

    // for packaging build scripts writting in the kotlin dsl
    // such as our java conventions plugin.
    `kotlin-dsl`
}

repositories {
    // Use the plugin portal to be able
    // to depend on gradle plugins in our plugins
    gradlePluginPortal()
}
