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
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials.username = "mapbox"
            // Read from gradle.properties or environment variable
            credentials.password = providers.gradleProperty("MAPBOX_DOWNLOADS_TOKEN").getOrElse("")
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "teraGaurd"
include(":app")
