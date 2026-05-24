// NexClip - app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.myvideo.editor"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.myvideo.editor"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17 -O2 -fvisibility=hidden -fno-exceptions -fno-rtti"
                arguments += listOf(
                    "-DANDROID_STL=c++_static",
                    "-DCMAKE_BUILD_TYPE=Release"
                )
            }
        }

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }

        buildConfigField("String", "BUILD_TIME", "\"${System.currentTimeMillis()}\"")
        buildConfigField("String", "GIT_HASH", "\"local_build\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file("../keystore/nexclip.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = "nexclip"
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
        create("debug") {
            storeFile = file("../keystore/debug.jks")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    flavorDimensions += "tier"
    productFlavors {
        create("free") {
            dimension = "tier"
            applicationIdSuffix = ".free"
            versionNameSuffix = "-free"
        }
        create("pro") {
            dimension = "tier"
            applicationIdSuffix = ".pro"
            versionNameSuffix = "-pro"
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {
    // ===== AndroidX =====
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.6")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // ===== Compose =====
    implementation(platform("androidx.compose:compose-bom:2024.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")

    // ===== 网络 =====
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ===== JSON =====
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // ===== 图片加载 =====
    implementation("io.coil-kt:coil-compose:2.5.0")

    // ===== 音视频处理：FFmpeg =====
    implementation("com.arthenica:ffmpeg-kit-full:6.0-2")

    // ===== 视频播放：Media3 ExoPlayer =====
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")
    implementation("androidx.media3:media3-transformer:1.2.1")
    implementation("androidx.media3:media3-effect:1.2.1")
    implementation("com.google.android.exoplayer:exoplayer-core:2.19.1")

    // ===== GPU滤镜：GPUImage =====
    implementation("jp.co.cyberagent.android:gpuimage:2.1.0")

    // ===== AI推理：ONNX Runtime =====
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.16.3")

    // ===== AI推理：PyTorch Mobile =====
    implementation("org.pytorch:pytorch_android:2.1.0")
    implementation("org.pytorch:pytorch_android_torchvision:2.1.0")

    // ===== AI框架：MediaPipe =====
    implementation("com.google.mediapipe:tasks-vision:0.10.8")
    implementation("com.google.mediapipe:tasks-text:0.10.8")
    implementation("com.google.mediapipe:tasks-audio:0.10.8")

    // ===== 测试 =====
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
