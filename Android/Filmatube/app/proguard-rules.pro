# ── Filmatube R8 / ProGuard rules ────────────────────────────────────────────
# Most libraries (Compose, Hilt, Media3, Room, Retrofit, Firebase) ship their own
# consumer rules. The explicit rules below cover kotlinx.serialization (reflection-
# free but needs generated serializers kept) and our own serialized DTOs.

-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,Signature,InnerClasses,EnclosingMethod

# ── kotlinx.serialization (official rules) ────────────────────────────────────
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Our @Serializable DTOs (presign, playback, download meta) and their generated serializers.
-keep,includedescriptorclasses class com.filmatube.app.**$$serializer { *; }
-keepclassmembers class com.filmatube.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.filmatube.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Retrofit service interfaces (annotated methods) ───────────────────────────
-keepattributes RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep BuildConfig (read at runtime for WEB_API_BASE_URL).
-keep class com.filmatube.app.BuildConfig { *; }

# Suppress benign missing-class warnings from optional transitive deps.
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
