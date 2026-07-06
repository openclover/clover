// Standalone Gradle build for the clover-idea module.
// Invoked directly (./gradlew ...) or via the Maven wrapper pom (exec-maven-plugin).
// Uses the IntelliJ Platform Gradle Plugin, the officially supported way to build and
// test JetBrains plugins.

import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

plugins {
    id("org.jetbrains.intellij.platform.settings") version "2.17.0"
}

rootProject.name = "clover-idea"

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        // clover core (org.openclover:clover) and jtreemap come from the local Maven repo,
        // populated by the reactor build (`mvn install`) before the Gradle build runs.
        mavenLocal()
        intellijPlatform {
            defaultRepositories()
        }
    }
}
