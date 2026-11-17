plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.chaman.geckotest"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.chaman.browser.geckotest"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1-geckoview-test"
    }

    // Test APK is intentionally ARM64-only to keep the first GeckoView build smaller.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a")
            isUniversalApk = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("org.mozilla.geckoview:geckoview:160.0.20261104181552")
}
