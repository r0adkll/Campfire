# R8 Proguard Rules

# Jetpack Compose Tracing Data
-assumenosideeffects public class androidx.compose.runtime.ComposerKt {
   boolean isTraceInProgress();
   void traceEventStart(int,int,int,java.lang.String);
   void traceEventStart(int,java.lang.String);
   void traceEventEnd();
}

# Strip out all Logcat calls from release builds
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Don't obfuscate the MainActivity name since we use a string literal in our Widget
# to launch it when the DI graph is not available
-keepnames class app.campfire.android.MainActivity
