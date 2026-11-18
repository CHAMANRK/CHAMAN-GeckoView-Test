import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.chaman.geckotest"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.chaman.browser.geckotest"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0-handshake"
    }

    // Permanent signing key (same key as CHAMAN Downloader). CI secrets give env vars; without them debug key.
    val chamanKeystore = System.getenv("CHAMAN_KEYSTORE_FILE")?.let { file(it) }?.takeIf { it.exists() }
    signingConfigs {
        if (chamanKeystore != null) {
            create("chaman") {
                storeFile = chamanKeystore
                storePassword = System.getenv("CHAMAN_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("CHAMAN_KEY_ALIAS") ?: "chaman"
                keyPassword = System.getenv("CHAMAN_KEYSTORE_PASSWORD") // PKCS12: key password = store password
            }
        }
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

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("chaman") ?: signingConfigs.getByName("debug")
        }
    }
}

// GeckoView pulls androidx.core 1.19.1, which needs AGP 9.1+. We stay on AGP 8.13.2
// and pin core/core-ktx to 1.17.0 (same family Mozilla builds GeckoView with).
configurations.configureEach {
    resolutionStrategy {
        force("androidx.core:core:1.17.0", "androidx.core:core-ktx:1.17.0")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("org.mozilla.geckoview:geckoview:160.0.20261104181552")
}
