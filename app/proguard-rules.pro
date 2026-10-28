# KidLoop R8 / Proguard Rules

# 1. WebView JavaScript Interface bridge
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.kidloop.app.player.YouTubePlayerBridge {
    public *;
}

# 2. Data models and persistence
-keepclassmembers class com.kidloop.app.data.model.** { *; }
-keepclassmembers class * implements androidx.datastore.core.Serializer { *; }

# 3. Preserve source file and line numbers for stack traces
-keepattributes SourceFile,LineNumberTable

# 4. Compose rules (standard consumer rules apply, but ensure runtime stability)
-dontwarn androidx.compose.**
