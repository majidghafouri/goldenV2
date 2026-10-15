import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

// Release signing credentials (keystore.properties is gitignored)
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(FileInputStream(keystorePropertiesFile))
    }
}

// AppMetrica API key (local.properties is gitignored). Test key from AppMetrica docs used as fallback.
val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties().apply {
    if (localPropertiesFile.exists()) {
        load(FileInputStream(localPropertiesFile))
    }
}

/** Google's public sample AdMob app ID, used when ADMOB_APP_ID is not configured. */
val GOOGLE_SAMPLE_ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713"

android {
    namespace = "com.goldenv2.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.goldenv2.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 9
        versionName = "1.0.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        buildConfigField(
            "String",
            "APPMETRICA_API_KEY",
            "\"${localProperties.getProperty("APPMETRICA_API_KEY", "test")}\""
        )

        // Google Mobile Ads (AdMob) reads the app ID from the manifest. When it is not
        // configured the placeholder falls back to Google's public sample ID, which is
        // never used because AdmobAdProvider reports AdMob as unavailable.
        manifestPlaceholders["admobAppId"] =
            localProperties.getProperty("ADMOB_APP_ID", GOOGLE_SAMPLE_ADMOB_APP_ID)
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-Xskip-prerelease-check",
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi"
        )
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.12"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += listOf("META-INF/*.kotlin_module", "META-INF/DEPENDENCIES")
        }
        // The Xray binary is shipped as a JNI lib (libxray.so). Legacy packaging
        // extracts it to nativeLibraryDir on disk, which is required because the
        // binary is executed as a process (exec() is denied on app data dirs by
        // SELinux since Android 10, but allowed on apk_data_file).
        jniLibs {
            useLegacyPackaging = true
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk = true
        }
    }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:domain"))
    implementation(project(":core:network"))
    implementation(project(":core:vpn"))
    implementation(project(":core:ui"))
    implementation(project(":feature:home"))
    implementation(project(":feature:servers"))
    implementation(project(":feature:routing"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:logs"))

    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // WorkManager with Hilt-injected workers (subscription auto-refresh)
    implementation(libs.workmanager.runtime)
    implementation(libs.hilt.work)
    ksp(libs.hilt.ext.compiler)

    // Accompanist
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.systemuicontroller)

    // Coil
    implementation(libs.coil.compose)

    // Material Design (for XML themes)
    implementation(libs.material)

    // Google User Messaging Platform: IAB TCF certified consent form.
    // Required by Google's EU User Consent Policy for EEA, UK and Swiss traffic.
    implementation(libs.user.messaging.platform)

    // AppMetrica (users, retention, crashes, ad revenue)
    implementation(libs.appmetrica)

    // AppMetrica Push (pushes sent/delivered) — requires Firebase project,
    // see README. Safe no-op if Firebase is not configured.
    implementation(libs.appmetrica.push)
    implementation(libs.firebase.messaging)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.test.espresso)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.tooling)
    androidTestImplementation(libs.androidx.test.ext)
}