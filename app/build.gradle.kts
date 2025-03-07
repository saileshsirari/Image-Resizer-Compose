import org.gradle.kotlin.dsl.implementation
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
    alias(libs.plugins.kotlinSerialization)
//    alias(libs.plugins.roomPlugin)
//    alias(libs.plugins.kspAndroid)
}
fun getApiKey(): String {
    val fl = rootProject.file("api.properties")

    return try {
        val properties = Properties()
        properties.load(FileInputStream(fl))
        properties.getProperty("MAPS_TOKEN")
    } catch (e: Exception) {
        "\"DEBUG\""
    }
}
val allowAllFilesAccess: String
    get() {
        val fl = rootProject.file("app.properties")

        return try {
            val properties = Properties()
            properties.load(FileInputStream(fl))
            properties.getProperty("ALL_FILES_ACCESS")
        } catch (e: Exception) {
            "true"
        }
    }
android {
    namespace = "com.image.resizer.compose"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.image.resizer.compose"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            manifestPlaceholders["appProvider"] = "com.dot.gallery.debug.media_provider"
            buildConfigField("Boolean", "ALLOW_ALL_FILES_ACCESS", allowAllFilesAccess)
            buildConfigField("String", "MAPS_TOKEN", getApiKey())
            buildConfigField(
                "String",
                "CONTENT_AUTHORITY",
                "\"com.dot.gallery.debug.media_provider\""
            )
        }
        release {
            manifestPlaceholders += mapOf(
                "appProvider" to "com.dot.gallery.media_provider"
            )
            isMinifyEnabled = false
            manifestPlaceholders["appProvider"] = "com.dot.staging.debug.media_provider"
            buildConfigField(
                "String",
                "CONTENT_AUTHORITY",
                "\"com.dot.staging.debug.media_provider\""
            )
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
        freeCompilerArgs += "-Xcontext-receivers"
    }
    composeCompiler {
        featureFlags = setOf(
            ComposeFeatureFlag.OptimizeNonSkippingGroups
        )
        includeSourceInformation = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10" // Or the version you are using
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(libs.coil.compose)
    implementation(libs.accompanist.permissions)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.sketch.http.ktor)
    // Subsampling
    implementation(libs.zoomimage.sketch)
    // Pinch to zoom
    implementation(libs.pinchzoomgrid)
    // Composables - Core
    implementation(libs.core)
    // Composable - Scrollbar
    implementation(libs.lazycolumnscrollbar)
    // Jetpack Security
        implementation(libs.androidx.security.crypto)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.window.size)
//    implementation(libs.androidx.biometric)
    // Room
//    implementation(libs.room.runtime)
    // Kotlin Extensions and Coroutines support for Room
//    implementation(libs.room.ktx)
//    ksp(libs.room.compiler)


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
//    implementation(libs.compose.cropper)
//    implementation ("com.github.SmartToolFactory:Compose-Cropper:Tag")
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.vanniktech.android.image.cropper)
    val nav_version = "2.8.6"

    // Jetpack Compose integration
    implementation(libs.androidx.navigation.compose)
    implementation (libs.androidx.lifecycle.viewmodel.compose)
}