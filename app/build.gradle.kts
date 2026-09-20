plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.firebaseCrashlytics) apply false
}

val googleServicesJson = file("google-services.json")
if (googleServicesJson.exists()) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

android {
    namespace = "com.rahul.vibetube1"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.rahul.zyvotube"
        minSdk = 24
        targetSdk = 36

        versionCode = 12
        versionName = "1.1.4-test"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.addAll(setOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
    }

    signingConfigs {
        create("debugConfig") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            enableV1Signing = true
            enableV2Signing = true
        }

        create("release") {
            enableV1Signing = true
            enableV2Signing = true
            val keystorePath = System.getenv("VIBETUBE_KEYSTORE_PATH")
                ?: (project.findProperty("VIBETUBE_KEYSTORE_PATH") as? String)
                ?: "${rootDir}/VibeTube-release.jks"
            val releaseKeystoreFile = file(keystorePath)

            if (releaseKeystoreFile.exists()) {
                val storePass = System.getenv("VIBETUBE_STORE_PASSWORD")
                    ?: (project.findProperty("VIBETUBE_STORE_PASSWORD") as? String)
                val keyPass = System.getenv("VIBETUBE_KEY_PASSWORD")
                    ?: (project.findProperty("VIBETUBE_KEY_PASSWORD") as? String)

                if (storePass.isNullOrEmpty() || keyPass.isNullOrEmpty()) {
                    logger.warn(
                        "Production keystore '${releaseKeystoreFile.name}' was found at ${releaseKeystoreFile.absolutePath}, " +
                        "but VIBETUBE_STORE_PASSWORD and/or VIBETUBE_KEY_PASSWORD environment/project variables are missing! " +
                        "Skipping production release signing configuration."
                    )
                } else {
                    storeFile = releaseKeystoreFile
                    storePassword = storePass
                    keyAlias = System.getenv("VIBETUBE_KEY_ALIAS")
                        ?: (project.findProperty("VIBETUBE_KEY_ALIAS") as? String)
                        ?: "vibetube"
                    keyPassword = keyPass
                }
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debugConfig")
        }
        release {
            val releaseConfig = signingConfigs.getByName("release")
            if (releaseConfig.storeFile?.exists() == true && releaseConfig.storePassword != null) {
                signingConfig = releaseConfig
            } else {
                signingConfig = signingConfigs.getByName("debugConfig")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/*.properties"
            excludes += "/META-INF/*.kotlin_module"
        }
    }

    compileOptions {
        // Enforces core library desugaring for Java 17 capabilities on minSdk 24
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Explicitly enforce Java 17 bytecode compatibility for Kotlin compilation
kotlin {
    jvmToolchain(21)
}

dependencies {
    // Bridges modern Java APIs (like URLEncoder methods) down to Android 11 and below
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.composeMaterialIconsExtended)
    implementation(libs.datastore.preferences)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.androidx.core.splashscreen)
    implementation(libs.material)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.androidx.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Media3
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.ui)
    implementation(libs.media3.session)
    implementation(libs.media3.datasource.okhttp)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Navigation
    implementation(libs.navigation.compose)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // NewPipe Extractor
    implementation(libs.newpipe.extractor)

    // OkHttp
    implementation(libs.okhttp)
    implementation(libs.errorproneAnnotations)

    // JSON
    implementation(libs.gson)
    implementation(libs.kotlinx.serialization.json)


    // Ktor
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.ffmpeg.kit.audio)
    implementation(libs.timber)
}

tasks.register("cleanAndBuildSafeRelease") {
    group = "build"
    description = "Cleans the build directory, removes debug artifacts, and generates a safely signed Release APK."
    
    dependsOn("clean")
    dependsOn("assembleRelease")
    
    tasks.getByName("assembleRelease").mustRunAfter("clean")
    
    doFirst {
        println("Starting clean build. Deleting old build artifacts and debug APKs...")
    }
    
    doLast {
        val releaseConfig = android.signingConfigs.findByName("release")
        val isProductionSigned = releaseConfig?.storeFile?.exists() == true && !releaseConfig.storePassword.isNullOrBlank()

        println("=========================================================")
        if (isProductionSigned) {
            println("SUCCESS: Production Release APK generated!")
            println("Signing: Production keystore applied with V1 (Jar Signature) and V2 (Full APK Signature).")
        } else {
            println("NOTICE: Release build completed without production signing.")
            println("STATUS: No production release keystore was found or configured.")
            println("To sign a production release, configure VIBETUBE_KEYSTORE_PATH, VIBETUBE_STORE_PASSWORD, VIBETUBE_KEY_ALIAS, and VIBETUBE_KEY_PASSWORD.")
        }
        println("=========================================================")
    }
}