# SakhiCare ProGuard Rules for Production Release
-keep class com.sakhicare.app.data.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
-dontwarn okio.**
-dontwarn retrofit2.**
