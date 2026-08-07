pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Polar"
include(":app")

// Abdullajon1881/LiquidGlass — vendored; not yet on Maven Central.
includeBuild("third_party/LiquidGlass") {
    dependencySubstitution {
        substitute(module("io.github.abdullajon1881:liquidglass-compose"))
            .using(project(":liquidglass-compose"))
        substitute(module("io.github.abdullajon1881:liquidglass-core"))
            .using(project(":liquidglass-core"))
    }
}
