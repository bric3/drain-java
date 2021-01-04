import de.undercouch.gradle.tasks.download.Download

plugins {
    application
    id("de.undercouch.download") version("4.1.1")
}

repositories {
    jcenter()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    implementation("com.google.guava:guava:30.1-jre")
}

application {
    mainClass.set("com.github.bric3.drain.App")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

tasks.withType(JavaCompile::class) {
    options.release.set(11)
}

val downloadFile by tasks.registering(Download::class) {
    src("https://zenodo.org/record/3227177/files/SSH.tar.gz")
    dest(File(buildDir, "SSH.tar.gz"))
    onlyIfModified(true)
}

tasks.processTestResources {
    dependsOn(downloadFile)
    from(tarTree(File(buildDir, "SSH.tar.gz")))
}

tasks.test {
    useJUnitPlatform()
}

