# Consumer ProGuard rules for composeApp library
# Most libraries (Koin, Room, Ktor, OkHttp) bundle their own rules

# Keep WorkManager workers
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker

# Keep data classes used with Kotlinx Serialization
-keepclassmembers class com.bernaferrari.changedetection.repo.** {
    *;
}
