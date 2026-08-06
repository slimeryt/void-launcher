plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.voidlauncher.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.voidlauncher.app"
        minSdk = 26
        targetSdk = 35
        versionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 1002
        versionName = (project.findProperty("versionName") as String?) ?: "0.1.2"

        // Override with -PaccountApiBase=https://...
        val accountApiBase = (project.findProperty("accountApiBase") as String?)
            ?: "https://polar-accounts.slimer0935.workers.dev"
        buildConfigField("String", "ACCOUNT_API_BASE", "\"$accountApiBase\"")
    }

    signingConfigs {
        create("ota") {
            // Shared OTA key — same cert for local + CI so Updates never conflict
            storeFile = rootProject.file("signing/void-ota.jks")
            storePassword = "void-ota-store"
            keyAlias = "void"
            keyPassword = "void-ota-store"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("ota")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("ota")
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

    packaging {
        // Legacy packaging (compressed .so + extractNativeLibs=true) is far more
        // reliable for sideload/GitHub installs across OEM package installers.
        // useLegacyPackaging=false caused "package appears to be invalid" for users
        // installing Polar.apk outside adb / Play.
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.graphics:graphics-shapes:1.0.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("com.google.accompanist:accompanist-drawablepainter:0.36.0")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.36.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
