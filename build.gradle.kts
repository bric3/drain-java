/*
 * drain-java
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
import de.undercouch.gradle.tasks.download.Download

plugins {
    application
    id("de.undercouch.download") version ("5.0.1")
    id("com.github.johnrengelman.shadow") version ("7.1.2")
    id("com.github.ben-manes.versions") version ("0.38.0")

    id("com.github.hierynomus.license") version "0.16.1"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.guava)
    implementation(libs.picocli)
    implementation(libs.bundles.jackson)

    annotationProcessor(libs.picocli.codegen)

    testImplementation(libs.assertj.core)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

application {
    mainClass.set("com.github.bric3.drain.Main")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

group = "com.github.bric3.drain"
version = "1.0-SNAPSHOT"


tasks {
    compileJava {
        options.compilerArgs.add("-Aproject=${project.group}/${project.name}")
    }

    withType(JavaCompile::class) {
        options.release.set(11)
    }

    val downloadFile by registering(Download::class) {
        src("https://zenodo.org/record/3227177/files/SSH.tar.gz")
        dest(File(buildDir, "SSH.tar.gz"))
        onlyIfModified(true)
    }

    processTestResources {
        dependsOn(downloadFile)
        from(tarTree(File(buildDir, "SSH.tar.gz")))
    }

    test {
        useJUnitPlatform()
        reports {
            junitXml.required.set(true)
            html.required.set(true)
        }
    }

    jar {
        manifest {
            attributes(
                    mapOf(
                            "Main-Class" to application.mainClass.get()
                    )
            )
        }
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
tasks.register("licenseCheckForProjectFiles", com.hierynomus.gradle.license.tasks.LicenseCheck::class) {
    source = fileTree(project.projectDir) {
        include("**/*.kt", "**/*.kts")
        include("**/*.toml")
        exclude("**/buildSrc/build/generated-sources/**")
    }
}
tasks["license"].dependsOn("licenseCheckForProjectFiles")
tasks.register("licenseFormatForProjectFiles", com.hierynomus.gradle.license.tasks.LicenseFormat::class) {
    source = fileTree(project.projectDir) {
        include("**/*.kt", "**/*.kts")
        include("**/*.toml")
        exclude("**/buildSrc/build/generated-sources/**")
    }
}
tasks["licenseFormat"].dependsOn("licenseFormatForProjectFiles")
