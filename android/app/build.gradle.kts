import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Load BACKEND_URL from ../config.properties so the parent can edit one file
// and rebuild. CI overrides via env var BACKEND_URL.
val backendUrl: String = run {
    val envOverride = System.getenv("BACKEND_URL")
    if (!envOverride.isNullOrBlank()) return@run envOverride
    val f = rootProject.file("config.properties")
    if (!f.exists()) "https://example.com"
    else Properties().apply { f.inputStream().use { load(it) } }
        .getProperty("BACKEND_URL", "https://example.com")
}

android {
    namespace = "com.family.kidstube"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.family.kidstube"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        vectorDrawables { useSupportLibrary = true }
        buildConfigField("String", "BACKEND_URL", "\"${backendUrl.trimEnd('/')}\"")
    }

    buildTypes {
        getByName("debug") { isMinifyEnabled = false }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    packaging { resources.excludes += setOf("META-INF/AL2.0", "META-INF/LGPL2.1") }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Image loading w/ HTTP cache
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")

    // Preferences (DataStore)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // YouTube IFrame Player wrapper (most maintained)
    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:12.1.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
