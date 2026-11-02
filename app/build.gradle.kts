plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Signing config đọc từ keystore.properties (không nằm trong repo, đã gitignore).
// Tạo tệp `keystore.properties` ở thư mục gốc dự án:
//   storeFile=path/to/keystore.jks
//   storePassword=...
//   keyAlias=...
//   keyPassword=...
// Nếu thiếu tệp này, release build vẫn comp được nhưng APK không ký (chỉ dùng gỡ lỗi).
val keystoreProps = java.util.Properties()
val keystoreFile = rootProject.file("keystore.properties")
if (keystoreFile.exists()) {
    keystoreProps.load(java.io.FileInputStream(keystoreFile))
}

android {
    namespace = "com.norr.music"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.norr.music"
        minSdk = 26
        targetSdk = 35
        versionCode = 21
        versionName = "3.9"
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile(file(keystoreProps.getProperty("storeFile")))
                storePassword(keystoreProps.getProperty("storePassword"))
                keyAlias(keystoreProps.getProperty("keyAlias"))
                keyPassword(keystoreProps.getProperty("keyPassword"))
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystoreProps.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
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
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("androidx.media3:media3-exoplayer:1.5.0")
    implementation("androidx.media3:media3-session:1.5.0")

    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("com.mpatric:mp3agic:0.9.1")
    implementation("net.jthink:jaudiotagger:3.0.1")

    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
