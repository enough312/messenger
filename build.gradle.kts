plugins {
    kotlin("android") version "1.9.24" apply false
    kotlin("multiplatform") version "1.9.24" apply false
    kotlin("plugin.serialization") version "1.9.24" apply false
    kotlin("jvm") version "1.9.24" apply false
    id("com.android.application") version "8.4.2" apply false
    id("com.android.library") version "8.4.2" apply false
    id("org.jetbrains.compose") version "1.6.11" apply false
    id("app.cash.sqldelight") version "2.0.2" apply false
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
}

allprojects {
    group = "com.messenger"
    version = "1.0.0"
}
