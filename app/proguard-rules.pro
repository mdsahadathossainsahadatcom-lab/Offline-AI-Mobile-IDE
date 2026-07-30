# ProGuard / R8 Rules for Mobile IDE & GGUF Local Model Engine

# Preserve Line Numbers & Source File Names for Stack Traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve Annotations, Signatures, and InnerClasses for Reflection / Serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Native JNI Methods and Classes with Native Library Bindings
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep C++ NDK / JNI Bridge and GGUF Native Execution Engine
-keep class com.example.engine.** { *; }
-keepclassmembers class com.example.engine.** {
    *;
}

# Explicitly Keep Engine Agent, Tool Execution Architecture, & State
-keep class com.example.engine.agent.** { *; }
-keepclassmembers class com.example.engine.agent.** {
    *;
}

# Explicitly Keep GGUF Header Parser, Tensor Metadata, & Inference Data Models
-keep class com.example.engine.gguf.** { *; }
-keepclassmembers class com.example.engine.gguf.** {
    *;
}
-keep class com.example.engine.inference.** { *; }
-keepclassmembers class com.example.engine.inference.** {
    *;
}

# Keep Native Library Loaders and System.loadLibrary Targets
-keepclassmembers class * {
    public static void main(java.lang.String[]);
    public static void loadLibrary(java.lang.String);
}

# Keep Room Database Entities, DAOs, Migrations, and Database Classes
-keep class com.example.data.db.** { *; }
-keepclassmembers class com.example.data.db.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.migration.Migration

# Keep Data Models, Repositories, and Utilities
-keep class com.example.data.** { *; }
-keepclassmembers class com.example.data.** { *; }
-keep class com.example.util.** { *; }
-keepclassmembers class com.example.util.** { *; }

# Keep ViewModels, UI States, and Composables
-keep class com.example.ui.viewmodel.** { *; }
-keepclassmembers class com.example.ui.viewmodel.** { *; }
-keep class com.example.ui.components.** { *; }

# Kotlin Coroutines & Flow Support
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }

# Kotlinx Serialization Keep Rules
-keepattributes *Annotation*,ElementTarget
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Prevent Optimization / Renaming of Enum Constants in Engine and Database Models
-keepclassmembers enum com.example.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# GGUF / llama.cpp / GGML JNI Wrapper Classes & Native Symbol Keep Rules
-keep class org.llama.** { *; }
-keepclassmembers class org.llama.** { *; }
-keep class com.llama.** { *; }
-keepclassmembers class com.llama.** { *; }
-keep class de.kramer.llama.** { *; }
-keepclassmembers class de.kramer.llama.** { *; }
-keep class com.sm.llama.** { *; }
-keepclassmembers class com.sm.llama.** { *; }
-keep class net.java.dev.jna.** { *; }
-keepclassmembers class net.java.dev.jna.** { *; }

# Keep native methods and JNI callback structures for local inference engines
-keepclasseswithmembers class * {
    native <methods>;
}
-keepclassmembers class * {
    @androidx.annotation.Keep <methods>;
    @androidx.annotation.Keep <fields>;
}

