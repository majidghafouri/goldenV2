pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        mavenCentral()
    }
    plugins {
        id("com.android.application") version "8.5.0"
        id("com.android.library") version "8.5.0"
        id("org.jetbrains.kotlin.android") version "2.0.0"
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
        id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"
        id("org.jetbrains.kotlin.plugin.parcelize") version "2.0.0"
        id("com.google.dagger.hilt.android") version "2.52"
        id("com.google.devtools.ksp") version "2.0.0-1.0.24"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "GoldenV2"

include(":app")
include(":core:data")
include(":core:network")
include(":core:vpn")
include(":core:ui")
include(":core:domain")
include(":feature:home")
include(":feature:servers")
include(":feature:routing")
include(":feature:settings")
include(":feature:logs")