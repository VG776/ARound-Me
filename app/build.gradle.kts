plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.example.aroundme"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.aroundme"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Add this vectorDrawables block for Compose
        vectorDrawables {
            useSupportLibrary = true
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

    // Prevent compression of TensorFlow/ML Kit models
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            pickFirsts += listOf("META-INF/LICENSE.md", "META-INF/LICENSE-notice.md")
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
        // This enables Jetpack Compose
        compose = true
    }
}

dependencies {
    // 🧱 Core Android
    implementation("androidx.core:core-ktx:1.12.0")

    // 📷 CameraX
    implementation("androidx.camera:camera-core:1.3.0")
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")

    // 🧠 TensorFlow Lite (offline models)
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    // 🧩 ML Kit (optional lightweight detection)
    implementation("com.google.mlkit:object-detection:17.0.0")

    // --- JETPACK COMPOSE DEPENDENCIES ---
    // Use the Compose Bill of Materials (BoM) to manage Compose versions. [4]
    val composeBom = platform("androidx.compose:compose-bom:2024.05.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Core Compose libraries
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Integration with Activities
    implementation("androidx.activity:activity-compose:1.9.0")

    // Integration with Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.1")

    // Tooling support for Android Studio Previews
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    // --- END OF COMPOSE DEPENDENCIES ---


    // 🧪 Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    // Test dependencies for Compose
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}