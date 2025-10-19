// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // This is needed for the Kotlin DSL to work properly
    kotlin("jvm") version "1.9.0" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.25" apply false
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("com.google.dagger.hilt.android") version "2.56.1" apply false
    id("com.google.gms.google-services") version "4.4.3" apply false
    id("com.google.firebase.crashlytics") version "3.0.6" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}