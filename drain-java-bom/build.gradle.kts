plugins {
    `java-platform`
    `maven-publish`
}

description = "Drain Java - BOM"

dependencies {
    constraints {
        rootProject.subprojects {
            if (name.startsWith("drain-java-") && !name.endsWith("-bom")) {
                api("${group}:${name}:${version}")
            }
        }
    }
}
