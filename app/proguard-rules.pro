# Keep Hilt / serialization models used by the crash reporter
-keepattributes *Annotation*
-keep class com.hmx.shield.crash.model.** { *; }
-keep class kotlinx.serialization.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Keep line numbers in stack traces for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
