import java.util.Date
import java.text.SimpleDateFormat

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.zero.sentinel"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.zero.sentinel"
        minSdk = 26
        targetSdk = 34
        // Git-based Versioning
        fun getGitCommitCount(): Int {
            return try {
                val process = ProcessBuilder("git", "rev-list", "--count", "HEAD").start()
                process.inputStream.bufferedReader().readText().trim().toInt()
            } catch (e: Exception) {
                1 // Fallback
            }
        }

        fun getGitCommitHash(): String {
            return try {
                val process = ProcessBuilder("git", "rev-parse", "--short", "HEAD").start()
                process.inputStream.bufferedReader().readText().trim()
            } catch (e: Exception) {
                "unknown"
            }
        }

        val commitCount = getGitCommitCount()
        val commitHash = getGitCommitHash()

        versionCode = commitCount
        versionName = "2.$commitCount.$commitHash"


        
        // Room schema export location
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("release.jks")
            storePassword = System.getenv("SIGNING_STORE_PASSWORD")
            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
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
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }

    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                output.outputFileName = "CyperBot-${variant.versionName}-${variant.name}.apk"
            }
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4") // Reverting to stable known

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Network & JSON
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0") // Downgrade slightly to be safe or keep 2.13.2 if no error? Error didn't mention gson. Keeping 2.11.0 to be safe.

    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06") // Revert to alpha if 1.1.0 requires newer SDK (it might not, but let's be safe)

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-service:2.8.6")


}
