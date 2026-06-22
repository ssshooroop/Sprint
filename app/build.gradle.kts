plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("dagger.hilt.android.plugin")
}

android {
    namespace = "com.sprint.runner"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.sprint.runner"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.google.dagger:hilt-android:${rootProject.extra.get("hilt_version")}")
    
    // Wear OS
    implementation("androidx.wear:wear:1.3.0")
    implementation("androidx.wear.compose:compose-navigation:${rootProject.extra.get("wear_compose_version")}")
    implementation("androidx.wear.compose:compose-material:${rootProject.extra.get("wear_compose_version")}")
    implementation("androidx.wear.compose:compose-foundation:${rootProject.extra.get("wear_compose_version")}")

    // Compose
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui:${rootProject.extra.get("compose_ui_version")}")
    implementation("androidx.compose.ui:ui-tooling-preview:${rootProject.extra.get("compose_ui_version")}")
    implementation("androidx.compose.foundation:foundation:1.5.4")
    implementation("androidx.compose.material:material:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Dependency Injection
    implementation("com.google.dagger:hilt-android:${rootProject.extra.get("hilt_version")}")
    kapt("com.google.dagger:hilt-compiler:${rootProject.extra.get("hilt_version")}")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Database
    implementation("androidx.room:room-runtime:${rootProject.extra.get("room_version")}")
    implementation("androidx.room:room-ktx:${rootProject.extra.get("room_version")}")
    kapt("androidx.room:room-compiler:${rootProject.extra.get("room_version")}")

    // Settings storage (single source of truth shared across screens)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Location (Doppler speed for distance mode)
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Charts
    implementation("com.github.PhilJay:MPAndroidChart:3.1.0")
    implementation("com.github.tehras:charts:0.2.4-alpha")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:${rootProject.extra.get("compose_ui_version")}")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling:${rootProject.extra.get("compose_ui_version")}")
    debugImplementation("androidx.compose.ui:ui-test-manifest:${rootProject.extra.get("compose_ui_version")}")
}

kapt {
    correctErrorTypes = true
}
