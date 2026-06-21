plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
}

android {
    namespace = "mniroy.instaremote"
    compileSdk = 35

    defaultConfig {
        applicationId = "mniroy.instaremote"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        
        ndk {
            abiFilters += listOf("armeabi-v7a")
        }
    }

    packaging {
        jniLibs {
            excludes += listOf(
                "lib/arm64-v8a/libSnpeHtpV68Skel.so",
                "lib/arm64-v8a/libSnpeHtpV69Skel.so",
                "lib/arm64-v8a/libSnpeHtpV73Skel.so",
                "lib/arm64-v8a/libSnpeHtpV75Skel.so",
                "lib/arm64-v8a/libSnpeHtpV79Skel.so",
                "lib/arm64-v8a/libcalculator_skel.so",
                "lib/x86/*"
            )
        }
        resources {
            excludes += listOf(
                "META-INF/rxjava.properties"
            )
            pickFirsts += listOf(
                "lib/arm64-v8a/libc++_shared.so",
                "lib/armeabi-v7a/libc++_shared.so"
            )
        }
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
        kotlinCompilerExtensionVersion = "1.5.1"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    // Wear OS & Location
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("androidx.wear:wear:1.3.0")
    compileOnly("com.google.android.wearable:wearable:2.9.0")
    
    // Compose
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.wear.compose:compose-material:1.3.0")
    implementation("androidx.wear.compose:compose-foundation:1.3.0")
    implementation("androidx.compose.runtime:runtime-livedata:1.6.0")
    
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
}
