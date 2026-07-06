# Keep Compose
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Keep Cupertino
-keep class com.alexzhirkevich.** { *; }

# Keep Haze
-keep class dev.chrisbanes.haze.** { *; }
