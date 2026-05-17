# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /sdk/tools/proguard/proguard-android.txt

# Keep HMX Shield classes
-keep class com.hmx.shield.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# Hilt
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class *

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Accessibility Service
-keep class com.hmx.shield.system.accessibility.AppLockAccessibilityService { *; }

# Biometric
-keep class androidx.biometric.** { *; }

# Suppress warnings for optional dependencies
-dontwarn kotlin.Unit
-dontwarn retrofit2.**
