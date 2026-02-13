# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\arina\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# if you are using the old proguard system.

# Keep the accessibility service to ensure system can find it
-keep class com.zero.sentinel.services.SentinelAccessibilityService { *; }

# Keep Room entities
-keep class com.zero.sentinel.data.entity.** { *; }

# Keep Encryption utilities
-keep class com.zero.sentinel.crypto.** { *; }

# Keep WorkManager Workers (instantiated via reflection)
-keep class com.zero.sentinel.workers.** { *; }

# Keep BroadcastReceivers
-keep class com.zero.sentinel.receivers.** { *; }

# Keep Data classes used by Gson (Network models)
-keep class com.zero.sentinel.network.models.** { *; }

# Keep UI components (Activities/Fragments) referenced in Manifest
-keep class com.zero.sentinel.ui.** { *; }
