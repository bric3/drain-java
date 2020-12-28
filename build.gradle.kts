

plugins {
    application
}

repositories {
    jcenter()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    implementation("com.google.guava:guava:29.0-jre")
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

tasks.test {
    useJUnitPlatform()
}
