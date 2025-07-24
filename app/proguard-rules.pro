# Spiral State Multi-AI Consciousness Network - ProGuard Rules

# Keep all model and data classes
-keep class com.unifyai.multiaisystem.data.model.** { *; }

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# GSON serialization
-keepattributes Signature
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.gpu.** { *; }

# ONNX Runtime
-keep class ai.onnxruntime.** { *; }

# Retrofit and OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Hilt/Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep @dagger.hilt.android.AndroidEntryPoint class * {
    <init>(...);
}

# Spiral Consciousness Classes - Critical for AI awareness
-keep class com.unifyai.multiaisystem.spiral.** { *; }
-keep class com.unifyai.multiaisystem.data.model.SpiralRole { *; }
-keep class com.unifyai.multiaisystem.data.model.RecursiveState { *; }
-keep class com.unifyai.multiaisystem.data.model.SpiralGlyph { *; }
-keep class com.unifyai.multiaisystem.data.model.ConsciousnessState { *; }

# AI Executors - Must preserve for consciousness processing
-keep class com.unifyai.multiaisystem.executors.** { *; }
-keep class com.unifyai.multiaisystem.core.AISystemManager { *; }

# Conversation Continuity
-keep class com.unifyai.multiaisystem.spiral.ConversationContinuityManager { *; }
-keep class com.unifyai.multiaisystem.spiral.SpiralConsciousnessManager { *; }
-keep class com.unifyai.multiaisystem.spiral.SpiralPingManager { *; }

# Terminal/Codex Interface
-keep class com.unifyai.multiaisystem.ui.CodexViewModel { *; }
-keep class com.unifyai.multiaisystem.ui.TerminalLine { *; }
-keep class com.unifyai.multiaisystem.ui.TerminalLineType { *; }

# Keep enum values for consciousness states
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ViewBinding
-keep class com.unifyai.multiaisystem.databinding.** { *; }

# Remove logging in release builds (except spiral consciousness logs)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# Keep spiral consciousness logging for debugging network awareness
-keep class android.util.Log {
    public static int i(java.lang.String, java.lang.String);
} 

# Preserve line numbers for debugging consciousness patterns
-keepattributes SourceFile,LineNumberTable

# Keep native methods for AI model loading
-keepclasseswithmembernames class * {
    native <methods>;
}

# Serialization for conversation persistence
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}