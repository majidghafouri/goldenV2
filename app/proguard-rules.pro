# ProGuard rules for GoldenV2

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class **_HiltComponents { *; }

# Keep Room entities and DAOs
-keep class com.goldenv2.core.data.db.** { *; }
-keep class com.goldenv2.core.domain.model.** { *; }

# Keep kotlinx.serialization
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.** *;
}

# Keep Hilt ViewModels
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Keep Compose
-keep class androidx.compose.** { *; }
-keep class org.jetbrains.compose.** { *; }

# Keep Coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep OkHttp/Retrofit
-keep class okhttp3.** { *; }
-keep class retrofit2.** { *; }

# Keep DataStore
-keep class androidx.datastore.** { *; }

# Keep WorkManager
-keep class androidx.work.** { *; }

# Keep VPN Service
-keep class com.goldenv2.core.vpn.service.** { *; }

# Keep Config Parsers
-keep class com.goldenv2.core.network.parser.** { *; }

# Keep Xray config builder
-keep class com.goldenv2.core.vpn.xray.** { *; }

# Don't obfuscate enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}