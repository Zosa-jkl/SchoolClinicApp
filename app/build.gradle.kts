plugins {
    alias(libs.plugins.android.application)

    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.schoolclinicappointment"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.schoolclinicappointment"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Core dependencies
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("com.google.android.material:material:1.12.0")

    // Firebase dependencies
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    implementation("com.google.firebase:firebase-auth:21.0.5")  // Firebase Authentication SDK
    // Other Firebase dependencies if needed
    implementation("com.google.firebase:firebase-database:20.0.5")  // Optional Firebase Realtime Database
    implementation("com.google.firebase:firebase-firestore:24.0.0")  // Optional Firebase Firestore
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore:24.0.0")


    // Remove SQLite-specific dependencies (you no longer need the MySQL connector)
    // implementation(files("libs/mysql-connector-j-9.2.0.jar"))  // Remove this line for Firebase.

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}


apply(plugin = "com.google.gms.google-services")  // Apply Firebase plugin


