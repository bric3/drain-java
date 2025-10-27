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
    `maven-publish`
}

description = "Drain Java Jackson Serialization"

dependencies {
    implementation(projects.drainJavaCore)
    implementation(libs.jsr305)
    implementation(libs.bundles.jackson)

    testImplementation(libs.assertj.core)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks {
    processTestResources {
        dependsOn(rootProject.tasks.getByPath("unpackFile"))
    }

    test {
        useJUnitPlatform()
        reports {
            junitXml.required.set(true)
            html.required.set(true)
        }
    }
}