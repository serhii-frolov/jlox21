plugins {
    // Auto-downloads the JDK requested by the toolchain (e.g. arm64 JDK 21) if missing locally.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "jlox21"
