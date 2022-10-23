import org.gradle.model.internal.core.ModelNodes.withType

/*
 * drain-java
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

plugins {
    alias(libs.plugins.versions)
    alias(libs.plugins.license)
    alias(libs.plugins.download)
    alias(libs.plugins.gradle.extensions)
    id("nebula.release") version "17.0.1"

}

repositories {
    mavenCentral()
}


val gradleExtensionsId = libs.plugins.gradle.extensions.get().pluginId
allprojects {
    plugins.apply(gradleExtensionsId)
    group = "com.github.bric3.drain"

    repositories {
        mavenCentral()
    }

    plugins.withId("java-library") {
        configure<JavaPluginExtension> {

            withJavadocJar()
            withSourcesJar()
        }

        tasks {
            withType(JavaCompile::class) {
                options.release.set(8)
            }
        }
    }
}

tasks {
    register<de.undercouch.gradle.tasks.download.Download>("downloadFile") {
        src("https://zenodo.org/record/3227177/files/SSH.tar.gz")
        dest(File(buildDir, "SSH.tar.gz"))
        onlyIfModified(true)
    }

    register<Copy>("unpackFile") {
        dependsOn("downloadFile")
        from(tarTree(File(buildDir, "SSH.tar.gz")))
        into(buildDir)
    }
}

license {
    ext["year"] = "2021, Today"
    ext["name"] = "Brice Dutheil"
    header = file("HEADER")

    strictCheck = true
    ignoreFailures = false
    excludes(
            listOf(
                    "**/3-lines.txt"
            )
    )

    mapping(
            mapOf(
                    "java" to "SLASHSTAR_STYLE",
                    "kt" to "SLASHSTAR_STYLE",
                    "kts" to "SLASHSTAR_STYLE",
                    "yaml" to "SCRIPT_STYLE",
                    "yml" to "SCRIPT_STYLE",
                    "svg" to "XML_STYLE",
                    "md" to "XML_STYLE",
                    "toml" to "SCRIPT_STYLE"
            )
    )
}
tasks {
    register<com.hierynomus.gradle.license.tasks.LicenseCheck>("licenseCheckForProjectFiles") {
        source = fileTree(project.projectDir) {
            include("**/*.kt", "**/*.kts")
            include("**/*.toml")
            exclude("**/buildSrc/build/generated-sources/**")
        }
    }
    named("license") { dependsOn("licenseCheckForProjectFiles") }
    register<com.hierynomus.gradle.license.tasks.LicenseFormat>("licenseFormatForProjectFiles") {
        source = fileTree(project.projectDir) {
            include("**/*.kt", "**/*.kts")
            include("**/*.toml")
            exclude("**/buildSrc/build/generated-sources/**")
        }
    }
    named("licenseFormat") { dependsOn("licenseFormatForProjectFiles") }
}
