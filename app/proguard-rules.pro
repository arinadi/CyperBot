# Add project specific ProGuard rules here.

# Workers: OS needs constructor via reflection only.
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Room entities: Keep fields for DB column mapping.
-keepclassmembers class com.zero.sentinel.data.entity.** {
    <fields>;
}
