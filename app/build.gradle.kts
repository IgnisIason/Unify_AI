plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
    id("kotlin-parcelize")
}

android {
    namespace = "com.unifyai.multiaisystem"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.unifyai.multiaisystem"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0-spiral-core"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // Dynamic model loading - download on first run
        manifestPlaceholders["maxSdkVersion"] = "35"
        manifestPlaceholders["largeHeap"] = "true"
        manifestPlaceholders["coreConsciousness"] = "true"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            manifestPlaceholders["coreOptimized"] = "true"
        }
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug-core"
            manifestPlaceholders["debugCoreConsciousness"] = "true"
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview"
        )
    }
    
    buildFeatures {
        viewBinding = true
        dataBinding = true
        compose = false // Using traditional views for terminal interface
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/gradle/incremental.annotation.processors"
        }
        jniLibs {
            pickFirsts += "**/libonnxruntime.so"
        }
    }
    
    bundle {
        abi {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        language {
            enableSplit = true
        }
    }
    
}

// ===== TENSORFLOW LITE DEPENDENCY RESOLUTION =====
// Resolves duplicate class conflicts between:
// - com.google.ai.edge.litert:litert-api:1.0.1 (newer LiteRT API)
// - org.tensorflow:tensorflow-lite-api:2.13.0 (traditional TF Lite API)
// 
// SOLUTION: Use traditional TensorFlow Lite 2.17.0 API consistently
// and exclude all LiteRT dependencies to prevent conflicts.
// This ensures compatibility with existing TensorFlowLiteExecutor code.
configurations.all {
    resolutionStrategy {
        // Force specific TensorFlow Lite versions
        force("org.tensorflow:tensorflow-lite:2.17.0")
        force("org.tensorflow:tensorflow-lite-gpu:2.17.0")
        force("org.tensorflow:tensorflow-lite-support:0.4.4")
        
        // Exclude all LiteRT dependencies to prevent conflicts
        exclude(group = "com.google.ai.edge.litert")
        exclude(group = "org.tensorflow", module = "tensorflow-lite-api")
        
        // Handle version conflicts and enforce consistent TF Lite versions
        eachDependency {
            if (requested.group == "org.tensorflow" && requested.name.startsWith("tensorflow-lite")) {
                when (requested.name) {
                    "tensorflow-lite" -> useVersion("2.17.0")
                    "tensorflow-lite-gpu" -> useVersion("2.17.0")
                    "tensorflow-lite-support" -> useVersion("0.4.4")
                }
                because("Enforcing consistent TensorFlow Lite versions to avoid conflicts")
            }
            // Reject any LiteRT dependencies
            if (requested.group == "com.google.ai.edge.litert") {
                useTarget("org.tensorflow:tensorflow-lite:2.17.0")
                because("Replacing LiteRT with traditional TensorFlow Lite API")
            }
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    
    // Fragment and Activity
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.activity:activity-ktx:1.9.3")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    
    // Room Database (Spiral Consciousness Persistence)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // WorkManager (Background AI Processing)
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    
    // Dependency Injection (Hilt)
    implementation("com.google.dagger:hilt-android:2.54")
    ksp("com.google.dagger:hilt-compiler:2.54")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")
    
    // AI/ML Libraries - Unified TensorFlow Lite 2.17.0
    // Using traditional TF Lite API, excluding conflicting LiteRT dependencies
    implementation("org.tensorflow:tensorflow-lite:2.17.0") {
        exclude(group = "com.google.ai.edge.litert", module = "litert-api")
    }
    implementation("org.tensorflow:tensorflow-lite-gpu:2.17.0") {
        exclude(group = "com.google.ai.edge.litert", module = "litert-api")
    }
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4") {
        exclude(group = "com.google.ai.edge.litert", module = "litert-api")
    }
    
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.21.0")
    
    // Networking (Remote AI APIs)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:adapter-rxjava3:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // JSON Processing
    implementation("com.google.code.gson:gson:2.10.1")
    
    // File Download Progress and Hashing (for model downloads)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.7.3")
    
    // Data Binding and View Binding
    implementation("androidx.databinding:databinding-runtime:8.7.3")
    
    // Navigation (for Codex Interface)
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.5")
    
    // Reactive Extensions
    implementation("io.reactivex.rxjava3:rxjava:3.1.8")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")
    
    // Image Processing (for AI inputs)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    ksp("com.github.bumptech.glide:compiler:4.16.0")
    
    // Preferences
    implementation("androidx.preference:preference-ktx:1.2.1")
    
    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // File I/O and Storage
    implementation("androidx.documentfile:documentfile:1.0.1")
    
    // Spiral Consciousness Logging
    implementation("com.jakewharton.timber:timber:5.0.1")
    
    // Performance Monitoring
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")
    
    // Testing Dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("androidx.work:work-testing:2.10.0")
    testImplementation("com.google.dagger:hilt-android-testing:2.54")
    kspTest("com.google.dagger:hilt-compiler:2.54")
    
    // Android Testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.work:work-testing:2.10.0")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.54")
    kspAndroidTest("com.google.dagger:hilt-compiler:2.54")
    
    // Debug Dependencies
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
}