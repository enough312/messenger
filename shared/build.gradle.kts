plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

val enableAndroidTarget = providers.gradleProperty("enableAndroidTarget").orElse("true").get().toBoolean()

if (enableAndroidTarget) {
    apply(plugin = "com.android.library")
}

kotlin {
    if (enableAndroidTarget) {
        androidTarget()
    }
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

if (enableAndroidTarget) {
    configure<com.android.build.gradle.LibraryExtension> {
        namespace = "com.messenger.shared"
        compileSdk = 35
        defaultConfig {
            minSdk = 26
        }
    }
}
