# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve stack trace attributes for Play Console crash/ANR deobfuscation
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes Signature,InnerClasses,EnclosingMethod,Exceptions,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault

# Kotlin metadata & reflection support (required for Moshi KotlinJsonAdapterFactory)
-keep class kotlin.Metadata { *; }

# Keep Moshi data models, @Json fields, and generated JsonAdapters
-keep class com.example.data.model.** { *; }
-keep class com.example.data.remote.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
    @com.squareup.moshi.Json <methods>;
}
-keep class * extends com.squareup.moshi.JsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
    public <init>(com.squareup.moshi.Moshi, java.lang.reflect.Type[]);
}

# Keep Room entities, DAOs, and database classes
-keep class com.example.data.local.** { *; }

# Keep enums whose names are stored in SharedPreferences (AppLanguage, AppThemeMode)
-keep enum com.example.** { *; }

# Keep Retrofit service method signatures
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep notification service and push helper classes for FCM service instantiation
-keep class com.example.notifications.** { *; }

# Suppress benign compile-time warnings for optional transitive annotations
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn okio.**
-dontwarn okhttp3.**
-dontwarn retrofit2.**
