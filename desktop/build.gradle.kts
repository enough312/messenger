plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
    id("app.cash.sqldelight")
}

kotlin {
    jvmToolchain(17)
}

compose.desktop {
    application {
        mainClass = "com.messenger.desktop.MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe)
            includeAllModules = true
            packageName = "Messenger"
            packageVersion = "1.0.7"
            description = "Messenger desktop client"
            vendor = "Messenger"
            windows {
                shortcut = true
                menu = true
                dirChooser = true
            }
        }
    }
}

sqldelight {
    databases {
        create("MessengerDatabase") {
            packageName.set("com.messenger.desktop.db")
        }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation("io.ktor:ktor-client-core-jvm:2.3.12")
    implementation("io.ktor:ktor-client-cio-jvm:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation-jvm:2.3.12")
    implementation("io.ktor:ktor-client-websockets-jvm:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.12")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
    implementation("io.insert-koin:koin-core:3.5.6")
    implementation("app.cash.sqldelight:sqlite-driver:2.0.2")
    implementation("app.cash.sqldelight:coroutines-extensions-jvm:2.0.2")
    testImplementation(kotlin("test"))
}
