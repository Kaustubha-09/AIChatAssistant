# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# Gson / data model DTOs
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.example.aichatassistant.data.model.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
