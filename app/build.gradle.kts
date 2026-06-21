plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt")
}

android {
    namespace = "com.arashivision.sdk.demo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.arashivision.sdk.demo"
        minSdk = 29
        targetSdk = 35
        versionCode = 61
        versionName = libs.versions.insta.get()
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += listOf("arm64-v8a")
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
                "lib/arm64-v8a/libc++_shared.so"
            )
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("./sdk.jks")
            storePassword = "insta360"
            keyAlias = "insta360"
            keyPassword = "insta360"
        }
    }

    // 3. 关键：告诉 Gradle CMakeLists.txt 的真实位置
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1" // 建议使用你 SDK Manager 中安装的具体版本
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }

    applicationVariants.configureEach {
        outputs.all {
            if (this is com.android.build.gradle.internal.api.BaseVariantOutputImpl) {
                outputFileName = "insta_sdk_demo_${buildType.name}_${versionName}.apk"
            }
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
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.preference)
    implementation(libs.preference.ktx)
    implementation(libs.material)
    implementation(libs.androidx.viewbinding)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.swiperefreshlayout)

    implementation(libs.xx.permissions)
    implementation(libs.flowlayout)
    implementation(libs.lottie)
    implementation(libs.glide)
    kapt(libs.glide.compiler)
    implementation(libs.immersionbar)
    implementation(libs.xlog)
    implementation(libs.filepicker)

    implementation(libs.insta.camera)
    implementation(libs.insta.media)

    implementation(files("libs/glide_transformations.jar"))
}