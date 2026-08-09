# ProGuard / R8 rules for FinalPlayer

# Preserve line numbers and source file attributes for stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve Annotations & Signatures
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep All Application Packages (Data, Domain, UI, DI, Preferences)
-keep class com.finalplayer.app.** { *; }
-keepclassmembers class com.finalplayer.app.** { *; }

# Jetpack Compose & Navigation
-keepclassmembers class * extends androidx.compose.ui.node.LayoutNode { *; }
-dontwarn androidx.compose.**
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# Kotlin Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ViewModels (Crucial for DI and Navigation)
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel { *; }

# CRUCIAL: Protect Parcelable and Serializable classes
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Room Database & KSP Generated Implementations
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.Dao { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class **.*_Impl { *; }
-dontwarn androidx.room.**

# Koin Dependency Injection
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# Media & MPV
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keep class is.xyz.mpv.** { *; }
-dontwarn is.xyz.mpv.**

# SMBJ & Apache Commons Net
-keep class com.hierynomus.** { *; }
-dontwarn com.hierynomus.**
-keep class org.apache.commons.net.** { *; }
-dontwarn org.apache.commons.net.**

# MBassador Event Bus (Missing javax.el classes on Android)
-dontwarn javax.el.**
-dontwarn java.beans.**
-keep class javax.el.** { *; }
-keep class net.engio.mbassy.** { *; }
-dontwarn net.engio.mbassy.**
