plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.kosmos"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.kosmos"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {

        debug {
            // Add debug-specific build config fields
            // Replace with your actual Google Cloud API key for Speech-to-Text
            buildConfigField("String", "GOOGLE_CLOUD_API_KEY", "\"${project.findProperty("GOOGLE_CLOUD_API_KEY") ?: ""}\"")
            buildConfigField("String", "SUPABASE_URL", "\"${project.findProperty("SUPABASE_URL") ?: ""}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${project.findProperty("SUPABASE_ANON_KEY") ?: ""}\"")
            buildConfigField("boolean", "ENABLE_LOGGING", "true")

            // Enable detailed logging for debugging
            isDebuggable = true
        }

        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Add your production API keys here
            buildConfigField("String", "GOOGLE_CLOUD_API_KEY", "\"${project.findProperty("GOOGLE_CLOUD_API_KEY_PROD") ?: project.findProperty("GOOGLE_CLOUD_API_KEY") ?: ""}\"")
            buildConfigField("String", "SUPABASE_URL", "\"${project.findProperty("SUPABASE_URL_PROD") ?: project.findProperty("SUPABASE_URL") ?: ""}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${project.findProperty("SUPABASE_ANON_KEY_PROD") ?: project.findProperty("SUPABASE_ANON_KEY") ?: ""}\"")
            buildConfigField("boolean", "ENABLE_LOGGING", "false")

            // Signing config for release
            //signingConfig signingConfigs.debug // Replace with actual signing config
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
            freeCompilerArgs.addAll(
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
            )
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM - this manages all Compose library versions
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Dependency Injection - Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.foundation)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Authentication (Google OAuth for Supabase)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Supabase
    // Note: gotrue-kt is included in compose-auth, no need to add separately
    implementation(libs.supabase.postgrest.kt)
    implementation(libs.supabase.storage.kt)
    implementation(libs.supabase.realtime.kt)
    implementation(libs.supabase.compose.auth)
    implementation(libs.supabase.compose.auth.ui)

    // Ktor for Supabase
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.utils)
    implementation(libs.ktor.client.okhttp) // OkHttp engine for WebSocket support

    // Networking
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp.logging.interceptor)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Image Loading
    implementation(libs.coil.compose)

    // Date Time
    implementation(libs.kotlinx.datetime)

    // Permissions
    implementation(libs.accompanist.permissions)

    // Browser for OAuth
    implementation(libs.androidx.browser)

    // Media & Audio (includes speech recognition capabilities)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)
    // Date Picker
    implementation("io.github.vanpra.compose-material-dialogs:datetime:0.9.0")
// Work Manager (for background tasks)
    implementation("androidx.work:work-runtime-ktx:2.11.0")
    implementation("androidx.hilt:hilt-work:1.3.0")
// Splash Screen
    implementation(libs.androidx.core.splashscreen)


    // Android Speech Recognition (Built-in alternative)
    //implementation("androidx.speech:speech:1.0.0-alpha06")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Debug implementations
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
// Performance
    implementation ("androidx.profileinstaller:profileinstaller:1.4.1")
// Security
    implementation("androidx.security:security-crypto:1.1.0")
}