# ─────────────────────────────────────────────────────
# ProGuard / R8 优化规则 — WalkFree KMP
# ─────────────────────────────────────────────────────

# Ktor Client
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.walkfree.model.**$$serializer { *; }
-keepclassmembers class com.walkfree.model.** { *** Companion; }
-keepclasseswithmembers class com.walkfree.model.** { kotlinx.serialization.KSerializer serializer(...); }

# Coil
-keep class coil3.** { *; }
-dontwarn coil3.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# WalkFree — keep model fields
-keepclassmembers class com.walkfree.model.** {
    <fields>;
}

# ── 性能优化 ─────────────────────────────────────────
# 移除日志 (Release)
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
