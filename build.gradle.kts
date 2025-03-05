// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}
// build.gradle.kts (Project level)
buildscript {
    repositories {
        google()  // Firebase repository
        mavenCentral()  // Maven repository
    }

    dependencies {
        classpath("com.google.gms:google-services:4.3.15")  // Firebase services classpath
    }
}





