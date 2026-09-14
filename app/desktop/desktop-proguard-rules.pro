# ProGuard rules for the desktop distribution.
#
# Shrink only — obfuscation stays off (see buildTypes.release.proguard in build.gradle.kts). The
# source is public under GPL-3.0, so renaming buys nothing and costs readable stack traces in
# every bug report.
#
# Unlike R8 on Android, the ProGuard run here does NOT pick up the consumer rules libraries ship
# in META-INF/proguard, so everything reflective has to be spelled out below.

-dontobfuscate

# Silences thousands of duplicate-META-INF/MANIFEST.MF notes from the ~260 input jars.
-dontnote

# Stack traces are the whole point of not obfuscating; keep what makes them readable.
-keepattributes Signature,
                Exceptions,
                InnerClasses,
                SourceFile,
                LineNumberTable,
                EnclosingMethod,
                *Annotation*,
                RuntimeVisibleAnnotations,
                RuntimeVisibleParameterAnnotations,
                RuntimeVisibleTypeAnnotations,
                AnnotationDefault

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}


# --- Kotlin -------------------------------------------------------------------------------------

-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-keep class kotlin.coroutines.Continuation
-dontwarn kotlin.**
-dontwarn kotlinx.**

# Kept whole, not trimmed. Shrinking this hierarchy drops super-interfaces that JobSupport's
# default-method calls still invokespecial against, and the app dies at startup with
# "Bad invokespecial instruction: interface method reference is in an indirect superinterface".
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# --- kotlinx.serialization ----------------------------------------------------------------------
#
# The compiler plugin generates a serializer per @Serializable class and the runtime finds it
# through the companion or a synthetic $serializer, neither of which is referenced in bytecode.

-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

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

-keep,includedescriptorclasses class app.campfire.**$$serializer { *; }
-keepclassmembers class app.campfire.** {
    *** Companion;
}
-keepclasseswithmembers class app.campfire.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Ktor ---------------------------------------------------------------------------------------

-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { volatile <fields>; }
-dontwarn io.ktor.**

# --- SQLDelight / SQLite ------------------------------------------------------------------------

-keep class org.sqlite.** { *; }
-keep class app.cash.sqldelight.** { *; }
-dontwarn org.sqlite.**

# --- JavaCPP / FFmpeg ---------------------------------------------------------------------------
#
# JavaCPP resolves every native binding by reading @Platform/@Properties annotations off classes it
# loads by name, so none of this is reachable by static analysis.

-keep class org.bytedeco.** { *; }
-keepattributes *Annotation*
-dontwarn org.bytedeco.**

# --- JNA (macOS Now Playing) --------------------------------------------------------------------
#
# JNA maps interfaces onto native libraries with dynamic proxies and reads field order reflectively
# for Structure subclasses.

-keep class com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.** { *; }
-keep class * implements com.sun.jna.Library { *; }
-keep class * implements com.sun.jna.Callback { *; }
-dontwarn com.sun.jna.**
-dontwarn java.awt.**

# --- Coil / okio / okhttp -------------------------------------------------------------------
#
# Coil discovers its fetchers and decoders through java.util.ServiceLoader, so nothing references
# them in bytecode and shrinking takes the lot. It then fails *silently* — every cover falls back
# to its placeholder with not one line in the log.

-keep class coil3.** { *; }
-keep class okio.** { *; }
-keep class okhttp3.** { *; }
-dontwarn coil3.**
-dontwarn okio.**
-dontwarn okhttp3.**

# --- Compose / skiko ----------------------------------------------------------------------------

-keep class org.jetbrains.skia.** { *; }
-keep class org.jetbrains.skiko.** { *; }
-dontwarn org.jetbrains.skiko.**

# --- Optional TLS providers ---------------------------------------------------------------------
#
# OkHttp and kotlincrypto reference BouncyCastle, Conscrypt and OpenJSSE so they can use them when
# present, and fall back to the platform provider when not. None is on the desktop classpath, and
# every one of ProGuard's missing-class warnings comes from here.

-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# --- Logging ------------------------------------------------------------------------------------

-dontwarn org.slf4j.**
-dontwarn ch.qos.logback.**

# --- Assorted optional dependencies that are never on the desktop classpath ----------------------

-dontwarn android.**
-dontwarn androidx.**
-dontwarn org.graalvm.**
-dontwarn com.oracle.svm.**
-dontwarn org.codehaus.mojo.**
-dontwarn javax.annotation.**
