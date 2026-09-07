pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "VodilEats"

// Core modules
include(":core:common")
include(":core:network")
include(":core:domain")
include(":core:database")
include(":core:ui")

// Feature modules
include(":feature:auth")
include(":feature:customer")
include(":feature:restaurant")
include(":feature:courier")

// App modules
include(":app-customer")
include(":app-restaurant")
include(":app-courier")
