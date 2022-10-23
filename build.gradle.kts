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
}

repositories {
    mavenCentral()
}

group = "com.github.bric3.drain"
version = "1.0-SNAPSHOT"

tasks.register<de.undercouch.gradle.tasks.download.Download>("downloadFile") {
    src("https://zenodo.org/record/3227177/files/SSH.tar.gz")
    dest(File(buildDir, "SSH.tar.gz"))
    onlyIfModified(true)
}

tasks.register<Copy>("unpackFile") {
    dependsOn("downloadFile")
    from(tarTree(File(buildDir, "SSH.tar.gz")))
    into(buildDir)
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
tasks.register<com.hierynomus.gradle.license.tasks.LicenseCheck>("licenseCheckForProjectFiles") {
    source = fileTree(project.projectDir) {
        include("**/*.kt", "**/*.kts")
        include("**/*.toml")
        exclude("**/buildSrc/build/generated-sources/**")
    }
}
tasks["license"].dependsOn("licenseCheckForProjectFiles")
tasks.register<com.hierynomus.gradle.license.tasks.LicenseFormat>("licenseFormatForProjectFiles") {
    source = fileTree(project.projectDir) {
        include("**/*.kt", "**/*.kts")
        include("**/*.toml")
        exclude("**/buildSrc/build/generated-sources/**")
    }
}
tasks["licenseFormat"].dependsOn("licenseFormatForProjectFiles")
