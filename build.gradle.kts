import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import de.undercouch.gradle.tasks.download.Download

plugins {
    application
    id("de.undercouch.download") version ("4.1.1")
    id("com.github.johnrengelman.shadow") version ("6.1.0")
    id("com.github.ben-manes.versions") version ("0.36.0")
}

repositories {
    jcenter()

    maven {
        setUrl("https://repo.gradle.org/gradle/libs/")
    }
}

dependencies {
    implementation("net.rubygrapefruit:file-events:0.22-milestone-10")
    implementation("net.rubygrapefruit:native-platform:0.22-milestone-10")

    implementation("net.rubygrapefruit:native-platform-test:0.22-milestone-10")
    implementation ("net.sf.jopt-simple:jopt-simple:5.0.4")

    implementation("com.google.guava:guava:30.1-jre")
    implementation("info.picocli:picocli:4.6.1")
    annotationProcessor("info.picocli:picocli-codegen:4.6.1")

    testImplementation("org.assertj:assertj-core:3.18.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

application {
    mainClass.set("com.github.bric3.drain.Main")
    mainClassName = mainClass.get() // for shadow plugin
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
            junitXml.setEnabled(true)
            html.setEnabled(true)
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

    named("dependencyUpdates", DependencyUpdatesTask::class.java).configure {
        fun isNonStable(version: String): Boolean {
            val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
            val regex = "^[0-9,.v-]+(-r)?$".toRegex()
            val isStable = stableKeyword || regex.matches(version)
            return isStable.not()
        }

        fun isSnapshot(version: String): Boolean {
            return listOf("SNAPSHOT", "ALPHA").any { version.toUpperCase().contains(it) }
        }

        // Example 2: disallow release candidates as upgradable versions from stable versions
        rejectVersionIf {
            isSnapshot(candidate.version) || (isNonStable(candidate.version) && !isNonStable(currentVersion))
        }
    }
}
