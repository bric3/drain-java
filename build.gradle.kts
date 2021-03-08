import de.undercouch.gradle.tasks.download.Download

plugins {
    application
    id("de.undercouch.download") version ("4.1.1")
    id("com.github.johnrengelman.shadow") version ("6.1.0")
}

repositories {
    jcenter()
}

dependencies {
    implementation("com.google.guava:guava:30.1-jre")
    implementation("info.picocli:picocli:4.6.1")
    implementation("com.fasterxml.jackson.core:jackson-core:2.4.1")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.4.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.4.1")
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
}



