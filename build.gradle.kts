/*
 * drain-java
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
import nebula.plugin.release.git.opinion.Strategies

fun properties(key: String, defaultValue: Any? = null) = (project.findProperty(key) ?: defaultValue).toString()

plugins {
    alias(libs.plugins.versions)
    alias(libs.plugins.license)
    alias(libs.plugins.download)
    alias(libs.plugins.gradle.extensions) apply false
    alias(libs.plugins.nebula.release)
//    id("nebula.release") version "18.0.8"

}

repositories {
    mavenCentral()
}

release {
    defaultVersionStrategy = Strategies.getSNAPSHOT()
}

val gradleExtensionsId = libs.plugins.gradle.extensions.get().pluginId
allprojects {
    plugins.apply(gradleExtensionsId)
    group = "io.github.bric3.drain"

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


    // Releasing testable with
    //  CI=true ORG_GRADLE_PROJECT_ossrhUsername=bric3 ORG_GRADLE_PROJECT_ossrhPassword=$(echo bad) ORG_GRADLE_PROJECT_signingKey=$(cat secring.gpg) ORG_GRADLE_PROJECT_signingPassword=$(cat passphrase) ./gradlew snapshot --console=verbose
    plugins.withId("maven-publish") {
        plugins.apply("signing")
        rootProject.tasks.release {
            dependsOn(tasks.named("publish"))
        }

        configure<PublishingExtension> {
            publications {
                register<MavenPublication>("maven") {
                    plugins.withId("java-platform") {
                        from(components["javaPlatform"])
                    }
                    plugins.withId("java-library") {
                        from(components["java"])
                    }
                    // OSSRH enforces the `-SNAPSHOT` suffix on snapshot repository
                    // https://central.sonatype.org/faq/400-error/#question
                    
                    pom {
                        afterEvaluate {
                            pom.description.set(project.description)
                        }
                        name.set(project.name)
                        url.set("https://github.com/bric3/drain-java")
                        licenses {
                            license {
                                distribution.set("repo")
                                name.set("Mozilla Public License 2.0")
                                url.set("https://www.mozilla.org/en-US/MPL/2.0/")
                            }
                        }
                        val gitRepo = "https://github.com/bric3/drain-java"
                        scm {
                            connection.set("scm:git:${gitRepo}.git")
                            developerConnection.set("scm:git:${gitRepo}.git")
                            url.set(gitRepo)
                        }
                        issueManagement {
                            system.set("GitHub")
                            url.set("${gitRepo}/issues")
                        }
                        developers {
                            developer {
                                id.set("bric3")
                                name.set("Brice Dutheil")
                                email.set("brice.dutheil@gmail.com")
                            }
                        }
                    }
                }
            }
            repositories {
                fun isSnapshot(version: Any) = version.toString().endsWith("-SNAPSHOT") || version.toString().contains("-dev")

                maven {
                    if (properties("publish.central").toBoolean()) {
                        val isGithubRelease = providers.environmentVariable("GITHUB_EVENT_NAME").orNull.equals("release", true)
                        name = "central"
                        url = uri(when {
                            isGithubRelease && !isSnapshot(project.version) -> "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
                            else -> "https://s01.oss.sonatype.org/content/repositories/snapshots"
                        })
                        credentials {
                            username = findProperty("ossrhUsername") as? String
                            password = findProperty("ossrhPassword") as? String
                        }
                    } else {
                        name = "build-dir"
                        url = uri("${rootProject.buildDir}/publishing-repository")
                    }
                    project.extra["publishingRepositoryUrl"] = url
                }
            }
        }

        val licenseSpec = copySpec {
            from("${project.rootDir}/LICENSE")
        }

        tasks {
            named("clean") {
                doLast {
                    delete("${project.buildDir}/publishing-repository")
                }
            }
            withType(Jar::class) {
                metaInf.with(licenseSpec)
                manifest.attributes(
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to project.version,
                    "Automatic-Module-Name" to project.name.replace('-', '.'),
                    "Created-By" to "${System.getProperty("java.version")} (${System.getProperty("java.specification.vendor")})",
                )
            }

            withType(Javadoc::class) {
                val options = options as StandardJavadocDocletOptions
                options.addStringOption("Xdoclint:none", "-quiet")
            }

            withType<Sign>().configureEach {
                onlyIf { System.getenv("CI") == "true" }
            }

            named("publish") {
                doFirst {
                    logger.lifecycle("Uploading '${project.name}:${project.version}' to ${project.extra["publishingRepositoryUrl"]}")
                }
            }
        }

        // Doc https://docs.gradle.org/current/userguide/signing_plugin.html#sec:signatory_credentials
        // Details in https://github.com/bric3/fireplace/issues/25
        configure<SigningExtension> {
            setRequired({ gradle.taskGraph.hasTask("publish") })
            useInMemoryPgpKeys(findProperty("signingKey") as? String, findProperty("signingPassword") as? String)
            sign(the<PublishingExtension>().publications["maven"])
        }
    }
}

tasks {
    register("v") {
        doLast {
            println("Version : ${project.version}")
        }
    }

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
