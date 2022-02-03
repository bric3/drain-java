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
    `java-library`
    alias(libs.plugins.download)
//    alias(libs.plugins.shadow)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.jsr305)
    implementation(libs.bundles.jackson)

    testImplementation(libs.assertj.core)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

tasks {
    compileJava {
        options.compilerArgs.add("-Aproject=${project.group}/${project.name}")
    }

    withType(JavaCompile::class) {
        options.release.set(11)
    }

//    val downloadFile by registering(de.undercouch.gradle.tasks.download.Download::class) {
//        src("https://zenodo.org/record/3227177/files/SSH.tar.gz")
//        dest(File(buildDir, "SSH.tar.gz"))
//        onlyIfModified(true)
//    }

    processTestResources {
        dependsOn(rootProject.tasks.getByPath("unpackFile"))
//        dependsOn(rootProject.tasks.getByPath("downloadFile"))
//        from(tarTree(File(buildDir, "SSH.tar.gz")))
    }

    test {
        useJUnitPlatform()
        reports {
            junitXml.required.set(true)
            html.required.set(true)
        }
    }
}