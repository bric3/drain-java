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
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("com.gradle.develocity") version "4.2"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "drain-java"

include(
    "drain-java-bom",
    "drain-java-core",
    "drain-java-jackson",
    "tailer"
)

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
        // termsOfUseAgree is handled by .gradle/init.d/configure-develocity.init.gradle.kts
    }
}