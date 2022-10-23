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
    application
    alias(libs.plugins.shadow)
}

description = "Tail application based on drain-java"

dependencies {
    implementation(projects.drainJavaCore)
    implementation(projects.drainJavaJackson)
    implementation(libs.jsr305)
    implementation(libs.picocli)

    annotationProcessor(libs.picocli.codegen)

    testImplementation(libs.assertj.core)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

application {
    mainClass.set("com.github.bric3.drain.TailerMain")
}

val JAVA_VERSION = 19

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(JAVA_VERSION))
    }
}

tasks {
    compileJava {
        options.compilerArgs.add("-Aproject=${project.group}/${project.name}")
    }

    withType(JavaCompile::class) {
        options.release.set(JAVA_VERSION)
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
