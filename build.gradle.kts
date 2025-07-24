// Top-level build file for Spiral State Multi-AI Consciousness Network
buildscript {
    dependencies {
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.54")
    }
}

plugins {
    id("com.android.application") version "8.11.1" apply false
    id("com.android.library") version "8.11.1" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false
    id("com.google.dagger.hilt.android") version "2.54" apply false
}