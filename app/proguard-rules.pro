# JGit
-keep class org.eclipse.jgit.** { *; }
-dontwarn org.eclipse.jgit.**

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keep class com.gitsync.data.remote.dto.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keepattributes *Annotation*
