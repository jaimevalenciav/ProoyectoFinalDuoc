plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.compose)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions { jvmTarget = "17" }
        }
    }
    sourceSets {
        androidMain.dependencies {
            implementation(projects.shared)
            // Compose
            implementation(platform(libs.compose.bom))
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.tooling)
            implementation(libs.compose.material3)
            implementation(libs.compose.icons)
            implementation(libs.activity.compose)
            implementation(libs.viewmodel.compose)
            implementation(libs.navigation.compose)
            // Koin
            implementation(libs.koin.android)
            implementation(libs.koin.compose)
            // Auth
            implementation(libs.msal)
            // Camera + ML Kit
            implementation(libs.camerax.core)
            implementation(libs.camerax.camera2)
            implementation(libs.camerax.lifecycle)
            implementation(libs.camerax.view)
            implementation(libs.mlkit.barcode)
            // Location
            implementation(libs.play.services.location)
            // Coroutines
            implementation(libs.kotlinx.coroutines.android)
        }
    }
}

android {
    namespace = "cl.fleetmanager.android"
    compileSdk = 35
    defaultConfig {
        applicationId = "cl.fleetmanager.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
